import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javaparser.ParseResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JavaParserTool {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println(
                "Usage: java -cp javaparser.jar JavaParserTool <path>");
            System.exit(1);
        }

        File file = new File(args[0]);

        // ── Поддержка Java 17 (records, sealed, text blocks) ──
        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(
            ParserConfiguration.LanguageLevel.JAVA_17
        );
        JavaParser parser = new JavaParser(config);

        try {
            ParseResult<CompilationUnit> parseResult = parser.parse(file);
            if (!parseResult.isSuccessful()) {
                System.err.println("Failed to parse file: " + file);
                System.exit(1);
            }
            CompilationUnit cu = parseResult.getResult().get();

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode result = mapper.createObjectNode();

            // Package
            String packageName = "";
            if (cu.getPackageDeclaration().isPresent()) {
                packageName = cu.getPackageDeclaration()
                    .get().getNameAsString();
            }
            result.put("package", packageName);

            // Classes (включая Records!)
            ArrayNode classesNode = mapper.createArrayNode();

            // ── Обычные классы и интерфейсы ──
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
                classesNode.add(processClassOrInterface(cls, mapper));
            });

            // ── Records (Java 14+) ──
            cu.findAll(RecordDeclaration.class).forEach(rec -> {
                classesNode.add(processRecord(rec, mapper));
            });

            // ── Enums ──
            cu.findAll(EnumDeclaration.class).forEach(en -> {
                classesNode.add(processEnum(en, mapper));
            });

            result.set("classes", classesNode);
            System.out.println(mapper.writeValueAsString(result));

        } catch (Exception e) {
            System.err.println("Error parsing file: " + e.getMessage());
            System.exit(1);
        }
    }

    // ══════════════════════════════════════════
    // Process ClassOrInterfaceDeclaration
    // ══════════════════════════════════════════
    private static ObjectNode processClassOrInterface(
            ClassOrInterfaceDeclaration cls, ObjectMapper mapper) {

        ObjectNode classNode = mapper.createObjectNode();
        classNode.put("class_name", cls.getNameAsString());
        classNode.put("is_interface", cls.isInterface());
        classNode.put("is_record", false);
        classNode.put("is_enum", false);

        // Annotations
        List<String> annotations = new ArrayList<>();
        for (AnnotationExpr ann : cls.getAnnotations()) {
            annotations.add(ann.toString());
        }
        ArrayNode annotationsArray = mapper.createArrayNode();
        for (String ann : annotations) {
            annotationsArray.add(ann);
        }
        classNode.set("annotations", annotationsArray);

        // Classification
        classNode.put("is_controller",
            isAnnotationPresent(annotations,
                "@Controller", "@RestController"));
        classNode.put("is_exception_handler",
            isAnnotationPresent(annotations,
                "@ControllerAdvice", "@RestControllerAdvice"));
        classNode.put("is_service",
            isAnnotationPresent(annotations, "@Service"));
        classNode.put("is_repository",
            isAnnotationPresent(annotations, "@Repository"));
        classNode.put("is_component",
            isAnnotationPresent(annotations, "@Component"));
        classNode.put("is_configuration",
            isAnnotationPresent(annotations, "@Configuration"));
        classNode.put("is_feign_client",
            isAnnotationPresent(annotations, "@FeignClient"));

        // Implements
        ArrayNode implementsArray = mapper.createArrayNode();
        for (ClassOrInterfaceType impl : cls.getImplementedTypes()) {
            implementsArray.add(impl.getNameAsString());
        }
        classNode.set("implements", implementsArray);

        // Extends
        ArrayNode extendsArray = mapper.createArrayNode();
        for (ClassOrInterfaceType ext : cls.getExtendedTypes()) {
            extendsArray.add(ext.getNameAsString());
        }
        classNode.set("extends", extendsArray);

        // Fields
        classNode.set("fields", processFields(cls.getFields(), mapper));

        // Constructors
        classNode.set("constructors",
            processConstructors(cls.getConstructors(), mapper));

        // Methods
        classNode.set("methods",
            processMethods(cls, cls.getMethods(), mapper));

        return classNode;
    }

    // ══════════════════════════════════════════
    // Process RecordDeclaration (Java 14+)
    // ══════════════════════════════════════════
    private static ObjectNode processRecord(
            RecordDeclaration rec, ObjectMapper mapper) {

        ObjectNode classNode = mapper.createObjectNode();
        classNode.put("class_name", rec.getNameAsString());
        classNode.put("is_interface", false);
        classNode.put("is_record", true);
        classNode.put("is_enum", false);

        // Annotations
        List<String> annotations = new ArrayList<>();
        for (AnnotationExpr ann : rec.getAnnotations()) {
            annotations.add(ann.toString());
        }
        ArrayNode annotationsArray = mapper.createArrayNode();
        for (String ann : annotations) {
            annotationsArray.add(ann);
        }
        classNode.set("annotations", annotationsArray);

        // Classification (records can be DTOs)
        classNode.put("is_controller", false);
        classNode.put("is_exception_handler", false);
        classNode.put("is_service", false);
        classNode.put("is_repository", false);
        classNode.put("is_component",
            isAnnotationPresent(annotations, "@Component"));
        classNode.put("is_configuration", false);
        classNode.put("is_feign_client", false);

        // Implements
        ArrayNode implementsArray = mapper.createArrayNode();
        for (ClassOrInterfaceType impl : rec.getImplementedTypes()) {
            implementsArray.add(impl.getNameAsString());
        }
        classNode.set("implements", implementsArray);

        // Extends — records don't extend
        classNode.set("extends", mapper.createArrayNode());

        // Record parameters → fields
        ArrayNode fieldsNode = mapper.createArrayNode();
        rec.getParameters().forEach(param -> {
            ObjectNode fieldNode = mapper.createObjectNode();
            fieldNode.put("name", param.getNameAsString());
            fieldNode.put("type", param.getType().asString());
            fieldNode.put("is_final", true);  // record fields are final
            fieldNode.put("is_static", false);
            fieldNode.put("is_private", true);
            fieldNode.put("is_public", false);
            fieldNode.put("is_protected", false);
            fieldNode.put("is_autowired", false);
            fieldNode.put("is_value", false);

            ArrayNode paramAnns = mapper.createArrayNode();
            param.getAnnotations().forEach(ann ->
                paramAnns.add(ann.toString()));
            fieldNode.set("annotations", paramAnns);

            fieldsNode.add(fieldNode);
        });

        // Also add any explicit fields in the record body
        for (FieldDeclaration field : rec.getFields()) {
            for (VariableDeclarator var : field.getVariables()) {
                ObjectNode fieldNode = mapper.createObjectNode();
                fieldNode.put("name", var.getNameAsString());
                fieldNode.put("type", var.getType().asString());
                fieldNode.put("is_final", field.isFinal());
                fieldNode.put("is_static", field.isStatic());
                fieldNode.put("is_private", field.isPrivate());
                fieldNode.put("is_public", field.isPublic());
                fieldNode.put("is_protected", field.isProtected());

                List<String> fieldAnnotations = new ArrayList<>();
                for (AnnotationExpr ann : field.getAnnotations()) {
                    fieldAnnotations.add(ann.toString());
                }
                ArrayNode fieldAnnsArray = mapper.createArrayNode();
                for (String ann : fieldAnnotations) {
                    fieldAnnsArray.add(ann);
                }
                fieldNode.set("annotations", fieldAnnsArray);

                fieldNode.put("is_autowired",
                    isAnnotationPresent(fieldAnnotations,
                        "@Autowired", "@Inject", "@Resource"));
                fieldNode.put("is_value",
                    isAnnotationPresent(fieldAnnotations, "@Value"));

                fieldsNode.add(fieldNode);
            }
        }
        classNode.set("fields", fieldsNode);

        // Constructors
        classNode.set("constructors",
            processConstructors(rec.getConstructors(), mapper));

        // Methods
        classNode.set("methods",
            processMethods(null, rec.getMethods(), mapper));

        return classNode;
    }

    // ══════════════════════════════════════════
    // Process EnumDeclaration
    // ══════════════════════════════════════════
    private static ObjectNode processEnum(
            EnumDeclaration en, ObjectMapper mapper) {

        ObjectNode classNode = mapper.createObjectNode();
        classNode.put("class_name", en.getNameAsString());
        classNode.put("is_interface", false);
        classNode.put("is_record", false);
        classNode.put("is_enum", true);

        // Annotations
        List<String> annotations = new ArrayList<>();
        for (AnnotationExpr ann : en.getAnnotations()) {
            annotations.add(ann.toString());
        }
        ArrayNode annotationsArray = mapper.createArrayNode();
        for (String ann : annotations) {
            annotationsArray.add(ann);
        }
        classNode.set("annotations", annotationsArray);

        classNode.put("is_controller", false);
        classNode.put("is_exception_handler", false);
        classNode.put("is_service", false);
        classNode.put("is_repository", false);
        classNode.put("is_component", false);
        classNode.put("is_configuration", false);
        classNode.put("is_feign_client", false);

        classNode.set("implements", mapper.createArrayNode());
        classNode.set("extends", mapper.createArrayNode());

        // Enum constants as fields
        ArrayNode fieldsNode = mapper.createArrayNode();
        for (EnumConstantDeclaration constant : en.getEntries()) {
            ObjectNode fieldNode = mapper.createObjectNode();
            fieldNode.put("name", constant.getNameAsString());
            fieldNode.put("type", en.getNameAsString());
            fieldNode.put("is_final", true);
            fieldNode.put("is_static", true);
            fieldNode.put("is_public", true);
            fieldNode.put("is_private", false);
            fieldNode.put("is_protected", false);
            fieldNode.put("is_autowired", false);
            fieldNode.put("is_value", false);
            fieldNode.set("annotations", mapper.createArrayNode());
            fieldsNode.add(fieldNode);
        }

        // Regular fields in enum
        for (FieldDeclaration field : en.getFields()) {
            for (VariableDeclarator var : field.getVariables()) {
                ObjectNode fieldNode = mapper.createObjectNode();
                fieldNode.put("name", var.getNameAsString());
                fieldNode.put("type", var.getType().asString());
                fieldNode.put("is_final", field.isFinal());
                fieldNode.put("is_static", field.isStatic());
                fieldNode.put("is_private", field.isPrivate());
                fieldNode.put("is_public", field.isPublic());
                fieldNode.put("is_protected", field.isProtected());
                fieldNode.put("is_autowired", false);
                fieldNode.put("is_value", false);
                fieldNode.set("annotations", mapper.createArrayNode());
                fieldsNode.add(fieldNode);
            }
        }
        classNode.set("fields", fieldsNode);

        classNode.set("constructors",
            processConstructors(en.getConstructors(), mapper));
        classNode.set("methods",
            processMethods(null, en.getMethods(), mapper));

        return classNode;
    }

    // ══════════════════════════════════════════
    // Shared helpers
    // ══════════════════════════════════════════

    private static ArrayNode processFields(
            List<FieldDeclaration> fields, ObjectMapper mapper) {
        ArrayNode fieldsNode = mapper.createArrayNode();
        for (FieldDeclaration field : fields) {
            for (VariableDeclarator var : field.getVariables()) {
                ObjectNode fieldNode = mapper.createObjectNode();
                fieldNode.put("name", var.getNameAsString());
                fieldNode.put("type", var.getType().asString());
                fieldNode.put("is_final", field.isFinal());
                fieldNode.put("is_static", field.isStatic());
                fieldNode.put("is_private", field.isPrivate());
                fieldNode.put("is_public", field.isPublic());
                fieldNode.put("is_protected", field.isProtected());

                List<String> fieldAnnotations = new ArrayList<>();
                for (AnnotationExpr ann : field.getAnnotations()) {
                    fieldAnnotations.add(ann.toString());
                }
                ArrayNode fieldAnnotationsArray = mapper.createArrayNode();
                for (String ann : fieldAnnotations) {
                    fieldAnnotationsArray.add(ann);
                }
                fieldNode.set("annotations", fieldAnnotationsArray);

                boolean isAutowired = isAnnotationPresent(fieldAnnotations,
                    "@Autowired", "@Inject", "@Resource");
                boolean isValue = isAnnotationPresent(fieldAnnotations,
                    "@Value");
                fieldNode.put("is_autowired", isAutowired);
                fieldNode.put("is_value", isValue);

                fieldsNode.add(fieldNode);
            }
        }
        return fieldsNode;
    }

    private static ArrayNode processConstructors(
            List<ConstructorDeclaration> constructors,
            ObjectMapper mapper) {
        ArrayNode constructorsNode = mapper.createArrayNode();
        for (ConstructorDeclaration ctor : constructors) {
            ObjectNode ctorNode = mapper.createObjectNode();
            ctorNode.put("name", ctor.getNameAsString());

            ArrayNode paramsNode = mapper.createArrayNode();
            ctor.getParameters().forEach(param -> {
                ObjectNode paramNode = mapper.createObjectNode();
                paramNode.put("name", param.getNameAsString());
                paramNode.put("type", param.getType().asString());
                paramNode.put("is_final", param.isFinal());

                ArrayNode paramAnns = mapper.createArrayNode();
                param.getAnnotations().forEach(ann ->
                    paramAnns.add(ann.toString()));
                paramNode.set("annotations", paramAnns);

                paramsNode.add(paramNode);
            });
            ctorNode.set("parameters", paramsNode);

            ArrayNode ctorAnns = mapper.createArrayNode();
            ctor.getAnnotations().forEach(ann ->
                ctorAnns.add(ann.toString()));
            ctorNode.set("annotations", ctorAnns);

            constructorsNode.add(ctorNode);
        }
        return constructorsNode;
    }

    private static ArrayNode processMethods(
            ClassOrInterfaceDeclaration parentCls,
            List<MethodDeclaration> methods,
            ObjectMapper mapper) {
        ArrayNode methodsNode = mapper.createArrayNode();

        for (MethodDeclaration method : methods) {
            // Только методы непосредственно в этом классе
            if (parentCls != null
                && method.getParentNode().isPresent()
                && method.getParentNode().get() != parentCls) {
                continue;
            }

            ObjectNode methodNode = mapper.createObjectNode();
            methodNode.put("name", method.getNameAsString());
            methodNode.put("return_type", method.getType().asString());

            // Method annotations
            List<String> methodAnnotations = new ArrayList<>();
            for (AnnotationExpr ann : method.getAnnotations()) {
                methodAnnotations.add(ann.toString());
            }
            ArrayNode methodAnnotationsArray = mapper.createArrayNode();
            for (String ann : methodAnnotations) {
                methodAnnotationsArray.add(ann);
            }
            methodNode.set("annotations", methodAnnotationsArray);

            // Method parameters
            ArrayNode methodParamsNode = mapper.createArrayNode();
            method.getParameters().forEach(param -> {
                ObjectNode paramNode = mapper.createObjectNode();
                paramNode.put("name", param.getNameAsString());
                paramNode.put("type", param.getType().asString());

                ArrayNode paramAnns = mapper.createArrayNode();
                param.getAnnotations().forEach(ann ->
                    paramAnns.add(ann.toString()));
                paramNode.set("annotations", paramAnns);

                List<String> paramAnnotList =
                    toStringList(param.getAnnotations());

                boolean isRequestBody = isAnnotationPresent(
                    paramAnnotList, "@RequestBody");
                boolean isPathVariable = isAnnotationPresent(
                    paramAnnotList, "@PathVariable");
                boolean isRequestParam = isAnnotationPresent(
                    paramAnnotList, "@RequestParam");

                paramNode.put("is_request_body", isRequestBody);
                paramNode.put("is_path_variable", isPathVariable);
                paramNode.put("is_request_param", isRequestParam);

                if (isRequestBody) {
                    methodNode.put("request_body_type",
                        param.getType().asString());
                }

                methodParamsNode.add(paramNode);
            });
            methodNode.set("parameters", methodParamsNode);

            // HTTP method and path
            String httpMethod = null;
            String path = null;

            if (isAnnotationPresent(methodAnnotations, "@GetMapping")) {
                httpMethod = "GET";
                path = extractPath(methodAnnotations, "@GetMapping");
            } else if (isAnnotationPresent(
                    methodAnnotations, "@PostMapping")) {
                httpMethod = "POST";
                path = extractPath(methodAnnotations, "@PostMapping");
            } else if (isAnnotationPresent(
                    methodAnnotations, "@PutMapping")) {
                httpMethod = "PUT";
                path = extractPath(methodAnnotations, "@PutMapping");
            } else if (isAnnotationPresent(
                    methodAnnotations, "@DeleteMapping")) {
                httpMethod = "DELETE";
                path = extractPath(
                    methodAnnotations, "@DeleteMapping");
            } else if (isAnnotationPresent(
                    methodAnnotations, "@PatchMapping")) {
                httpMethod = "PATCH";
                path = extractPath(
                    methodAnnotations, "@PatchMapping");
            } else if (isAnnotationPresent(
                    methodAnnotations, "@RequestMapping")) {
                httpMethod = extractHttpMethodFromRequestMapping(
                    methodAnnotations);
                path = extractPath(
                    methodAnnotations, "@RequestMapping");
            }

            // Exception Handler
            boolean isExceptionHandler = isAnnotationPresent(
                methodAnnotations, "@ExceptionHandler");
            if (isExceptionHandler) {
                methodNode.put("is_exception_handler", true);
                ArrayNode exceptionTypesNode = mapper.createArrayNode();
                for (String ann : methodAnnotations) {
                    if (ann.contains("@ExceptionHandler")) {
                        String[] types = ann.split("[\\{\\},()]");
                        for (String t : types) {
                            t = t.trim();
                            if (t.endsWith(".class")) {
                                exceptionTypesNode.add(
                                    t.replace(".class", ""));
                            }
                        }
                    }
                }
                methodNode.set("exception_types", exceptionTypesNode);
            }

            // Response status
            if (isAnnotationPresent(
                    methodAnnotations, "@ResponseStatus")) {
                for (String ann : methodAnnotations) {
                    if (ann.contains("@ResponseStatus")) {
                        methodNode.put("response_status", ann);
                    }
                }
            }

            methodNode.put("http_method", httpMethod);
            methodNode.put("path", path);

            // Path params and query params
            ArrayNode pathParams = mapper.createArrayNode();
            ArrayNode queryParams = mapper.createArrayNode();
            method.getParameters().forEach(param -> {
                List<String> pAnns =
                    toStringList(param.getAnnotations());
                if (isAnnotationPresent(pAnns, "@PathVariable")) {
                    pathParams.add(param.getNameAsString());
                }
                if (isAnnotationPresent(pAnns, "@RequestParam")) {
                    queryParams.add(param.getNameAsString());
                }
            });
            methodNode.set("path_params", pathParams);
            methodNode.set("query_params", queryParams);

            methodsNode.add(methodNode);
        }
        return methodsNode;
    }

    // ══════════════════════════════════════════
    // Utility methods
    // ══════════════════════════════════════════

    private static boolean isAnnotationPresent(
            List<String> annotations, String... targetAnnotations) {
        for (String ann : annotations) {
            for (String target : targetAnnotations) {
                if (ann.contains(target)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String extractPath(
            List<String> annotations, String targetAnnotation) {
        for (String ann : annotations) {
            if (ann.contains(targetAnnotation)) {
                int start = ann.indexOf('"');
                if (start != -1) {
                    int end = ann.indexOf('"', start + 1);
                    if (end != -1) {
                        return ann.substring(start + 1, end);
                    }
                }
                if (ann.matches("@\\w+Mapping\\s*$")
                    || ann.matches("@\\w+Mapping\\(\\)")) {
                    return "";
                }
            }
        }
        return "";
    }

    private static String extractHttpMethodFromRequestMapping(
            List<String> annotations) {
        for (String ann : annotations) {
            if (ann.contains("@RequestMapping")) {
                String upper = ann.toUpperCase();
                if (upper.contains("POST")) return "POST";
                if (upper.contains("PUT")) return "PUT";
                if (upper.contains("DELETE")) return "DELETE";
                if (upper.contains("PATCH")) return "PATCH";
            }
        }
        return "GET";
    }

    private static List<String> toStringList(
            com.github.javaparser.ast.NodeList<AnnotationExpr>
                annotations) {
        List<String> result = new ArrayList<>();
        for (AnnotationExpr ann : annotations) {
            result.add(ann.toString());
        }
        return result;
    }
}