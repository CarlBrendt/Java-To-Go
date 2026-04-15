import os
import io
import asyncio
import logging
import tempfile
import zipfile
from minio import Minio
from minio.error import S3Error

from src.settings.config import APISettings

settings = APISettings()
logger = logging.getLogger(__name__)

class MinioService:
    def __init__(self) -> None:
        """
        Инициализация клиента MinIO.
        """
        self.client = Minio(
            endpoint=settings.minio_endpoint,
            access_key=settings.minio_access_key,
            secret_key=settings.minio_secret_key,
            secure=settings.minio_secure
        )

    def _make_bucket_if_not_exists(self, bucket_name: str) -> None:
        """Синхронная проверка и создание бакета."""
        try:
            if not self.client.bucket_exists(bucket_name):
                self.client.make_bucket(bucket_name)
                logger.info(f"Bucket '{bucket_name}' created.")
        except S3Error as e:
            if e.code != "BucketAlreadyOwnedByYou":
                logger.error(f"Error creating bucket {bucket_name}: {e}")
                raise e


    async def upload_file(self, source_file: str, bucket_name: str, user_id: str, original_filename: str = None):
        """
        Асинхронная загрузка файла.
        """
        # Используем get_running_loop() вместо deprecated get_event_loop()
        loop = asyncio.get_running_loop()
        
        def _sync_upload():
            # 1. Проверяем бакет
            self._make_bucket_if_not_exists(bucket_name)

            # 2. Логика определения имени файла
            # Важно: мы должны определить имя ДО использования в строке f-string
            file_name_to_use = original_filename
            if file_name_to_use is None:
                file_name_to_use = os.path.basename(source_file)
            
            # Формируем полное имя объекта
            destination_file = f"user_{user_id}_{file_name_to_use}"

            try:
                self.client.fput_object(
                    bucket_name, 
                    destination_file, 
                    source_file
                )
                logger.info(f"File {source_file} uploaded as {destination_file}")
                return destination_file
            except S3Error as e:
                logger.error(f"Error uploading file to {bucket_name}/{destination_file}: {e}")
                raise e

        return await loop.run_in_executor(None, _sync_upload)
    

    async def delete_file(self, bucket_name: str, object_name: str):
        """
        Удаляет один файл по имени объекта.
        
        :param bucket_name: Имя бакета.
        :param object_name: Полное имя объекта (включая путь, например: user_123_file.txt).
        """
        loop = asyncio.get_running_loop()
        
        def _sync_delete():
            try:
                self.client.remove_object(bucket_name, object_name)
                logger.info(f"File deleted: {object_name}")
                return True
            except S3Error as e:
                logger.error(f"Error deleting file {object_name}: {e}")
                raise e

        return await loop.run_in_executor(None, _sync_delete)


    async def delete_user_folder(self, bucket_name: str, user_id: str):
        """
        Удаляет ВСЮ папку пользователя со всем содержимым.
        Например: processed/user_11/ и raw/user_11/
        """
        loop = asyncio.get_running_loop()
        
        def _sync_delete_folder():
            try:
                # Проверяем существование бакета
                if not self.client.bucket_exists(bucket_name):
                    logger.warning(f"Bucket '{bucket_name}' does not exist")
                    return {"deleted": 0, "prefixes": []}

                prefixes = [
                    f"processed/user_{user_id}/",  # папка с обработанными файлами
                    f"raw/user_{user_id}"          # папка с raw файлами (без слеша для zip)
                ]
                
                total_deleted = 0
                deleted_prefixes = []
                
                for prefix in prefixes:
                    objects_to_delete = []
                    logger.info(f"Scanning for files with prefix: {prefix}")
                    
                    # Получаем ВСЕ объекты с этим префиксом
                    try:
                        objects = self.client.list_objects(
                            bucket_name, 
                            prefix=prefix, 
                            recursive=True
                        )
                        
                        for obj in objects:
                            objects_to_delete.append(obj.object_name)
                            logger.info(f"Found: {obj.object_name}")
                            
                    except S3Error as e:
                        logger.error(f"Error listing objects with prefix {prefix}: {e}")
                        continue
                    
                    if not objects_to_delete:
                        logger.info(f"No files found with prefix '{prefix}'")
                        continue
                    
                    logger.info(f"Found {len(objects_to_delete)} files to delete with prefix '{prefix}'")
                    
                    # Удаляем файлы
                    deleted_count = 0
                    for obj_name in objects_to_delete:
                        try:
                            self.client.remove_object(bucket_name, obj_name)
                            deleted_count += 1
                            logger.info(f"Deleted: {obj_name}")
                        except S3Error as e:
                            logger.error(f"Error deleting {obj_name}: {e}")
                    
                    total_deleted += deleted_count
                    if deleted_count > 0:
                        deleted_prefixes.append(prefix)
                    logger.info(f"Deleted {deleted_count} files from '{prefix}'")
                
                return {
                    "deleted": total_deleted,
                    "prefixes": deleted_prefixes
                }
                
            except Exception as e:
                logger.error(f"Error deleting user folder for user {user_id}: {e}")
                raise e
        
        return await loop.run_in_executor(None, _sync_delete_folder)
    

    async def upload_zip_and_extract(self, zip_file_path: str, bucket_name: str, user_id: str) -> dict:
        """
        Загружает ZIP в MinIO, распаковывает и загружает содержимое в MinIO.
        
        :param zip_file_path: Путь к локальному ZIP файлу (уже загруженному в /tmp).
        :param bucket_name: Имя бакета.
        :param user_id: ID пользователя.
        """
        loop = asyncio.get_running_loop()
        
        def _sync_process():
            # 1. Создаем временную папку для распаковки
            with tempfile.TemporaryDirectory() as temp_dir:
                try:
                    # 2. Распаковываем ZIP
                    with zipfile.ZipFile(zip_file_path, 'r') as zip_ref:
                        zip_ref.extractall(temp_dir)
                    
                    # 3. Получаем список всех файлов внутри распакованной папки
                    files_to_upload = []
                    for root, dirs, files in os.walk(temp_dir):
                        for file in files:
                            full_path = os.path.join(root, file)
                            # Получаем относительный путь внутри ZIP (например: folder/file.txt)
                            rel_path = os.path.relpath(full_path, temp_dir)
                            files_to_upload.append((full_path, rel_path))
                    
                    logger.info(f"Found {len(files_to_upload)} files to upload for user {user_id}")

                    # 4. Формируем префиксы
                    # ZIP файл лежит в: raw/user_{user_id}/filename.zip
                    zip_object_name = f"raw/user_{user_id}_{os.path.basename(zip_file_path)}"
                    
                    # Распакованные файлы лежат в: processed/user_{user_id}/{rel_path}
                    processed_prefix = f"processed/user_{user_id}"

                    # 5. Загружаем ZIP файл в MinIO
                    try:
                        self.client.fput_object(bucket_name, zip_object_name, zip_file_path)
                        logger.info(f"Uploaded ZIP: {zip_object_name}")
                    except S3Error as e:
                        logger.error(f"Error uploading ZIP: {e}")
                        raise e

                    # 6. Загружаем каждый распакнутый файл
                    for local_path, rel_path in files_to_upload:
                        # Формируем имя объекта: processed/user_11/folder/file.txt
                        object_name = f"{processed_prefix}/{rel_path}"
                        
                        # Оптимизация: проверяем существование, если нужно (опционально)
                        self.client.fput_object(bucket_name, object_name, local_path)
                        logger.debug(f"Uploaded: {object_name}")

                    return {
                        "status": "success",
                        "zip_name": zip_object_name,
                        "files_count": len(files_to_upload)
                    }

                except Exception as e:
                    logger.error(f"Error processing ZIP for user {user_id}: {e}")
                    raise e

        return await loop.run_in_executor(None, _sync_process)
    

    async def upload_zip_and_extract_in_memory(self, zip_bytes: bytes, original_filename: str, bucket_name: str, user_id: str):
        loop = asyncio.get_running_loop()
        
        def _sync_process():
            try:
                # 1. Создаем BytesIO объект из байтов
                zip_buffer = io.BytesIO(zip_bytes)
                zip_size = len(zip_bytes)

                # 2. Формируем имена объектов
                zip_object_name = f"raw/user_{user_id}_{original_filename}"
                processed_prefix = f"processed/user_{user_id}"

                # 3. Загружаем сам ZIP файл в MinIO
                self.client.put_object(
                    bucket_name,
                    zip_object_name,
                    zip_buffer,
                    zip_size
                )
                logger.info(f"Uploaded ZIP: {zip_object_name}")

                # 4. Распаковываем ZIP из буфера в память
                # Важно: после put_object буфер может быть "прочитан", поэтому создадим новый или перезапишем
                zip_buffer.seek(0) 

                with zipfile.ZipFile(zip_buffer, 'r') as zip_ref:
                    file_list = zip_ref.namelist()
                    logger.info(f"Found {len(file_list)} files to upload for user {user_id}")

                    for file_name in file_list:
                        # Пропускаем папки (завершаются на /)
                        if file_name.endswith('/'):
                            continue

                        # Читаем содержимое файла из архива
                        file_content = zip_ref.read(file_name)
                        file_size = len(file_content)

                        # Формируем имя объекта
                        object_name = f"{processed_prefix}/{file_name}"

                        # 5. Загружаем каждый файл в MinIO
                        # ИСПРАВЛЕНИЕ: Используем put_object для файлов в памяти
                        self.client.put_object(
                            bucket_name,
                            object_name,
                            io.BytesIO(file_content),
                            file_size
                        )
                        logger.debug(f"Uploaded: {object_name}")
                    logger.info("Zip file and its files in minio")
                    return {
                        "status": "success",
                        "zip_name": zip_object_name,
                        "files_count": len(file_list)
                    }

            except Exception as e:
                logger.error(f"Error processing ZIP for user {user_id}: {e}")
                raise e

        return await loop.run_in_executor(None, _sync_process)
    

    async def get_ready_zip_bytes(
        self,
        bucket_name:str,
        user_id:str,
        filename:str|None=None,
    )->tuple[bytes,str]:
        loop=asyncio.get_running_loop()

        def _sync_get()->tuple[bytes,str]:
            prefix=f"ready/user_{user_id}"
            prefix_dir=f"{prefix}/"

            def _read_object(object_name:str)->bytes:
                resp=self.client.get_object(bucket_name, object_name)
                try:
                    return resp.read()
                finally:
                    resp.close()
                    resp.release_conn()

            if filename:
                object_name=f"{prefix_dir}{filename}"
                try:
                    data=_read_object(object_name)
                    return data, os.path.basename(object_name)
                except S3Error as e:
                    if getattr(e, "code", None) in ("NoSuchKey", "NoSuchBucket"):
                        raise FileNotFoundError(
                            f"Объект не найден: {object_name}"
                        ) from e
                    logger.error(f"get_object {object_name}: {e}")
                    raise

            zip_keys:list[str]=[]
            try:
                for obj in self.client.list_objects(
                    bucket_name, prefix=prefix_dir, recursive=True
                ):
                    name=obj.object_name
                    if name.lower().endswith(".zip"):
                        zip_keys.append(name)
            except S3Error as e:
                logger.error(f"list_objects prefix={prefix_dir}: {e}")
                raise

            if not zip_keys:
                flat_key=f"{prefix}.zip"
                try:
                    data=_read_object(flat_key)
                    return data, os.path.basename(flat_key)
                except S3Error as e:
                    if getattr(e, "code", None) in ("NoSuchKey", "NoSuchBucket"):
                        raise FileNotFoundError(
                            f"Нет zip: ни под {prefix_dir!r}, ни {flat_key!r}"
                        ) from e
                    logger.error(f"get_object {flat_key}: {e}")
                    raise

            if len(zip_keys) > 1:
                zip_keys.sort()
                raise ValueError(
                    "Найдено несколько zip-файлов: "
                    f"{zip_keys!r}. Укажите query-параметр filename."
                )

            object_name=zip_keys[0]
            try:
                data=_read_object(object_name)
                return data, os.path.basename(object_name)
            except S3Error as e:
                logger.error(f"get_object {object_name}: {e}")
                raise

        return await loop.run_in_executor(None, _sync_get)
