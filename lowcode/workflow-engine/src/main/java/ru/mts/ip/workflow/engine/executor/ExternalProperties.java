package ru.mts.ip.workflow.engine.executor;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.exception.ClientError;
import ru.mts.ip.workflow.engine.exception.ClientErrorDescription;

@Data
@Accessors(chain = true)
public class ExternalProperties {
  private Map<String, String> encodedValuesMap = Map.of();
  private Map<String, ClientErrorDescription> fetchingErrors = Map.of();

  public FetchedSecret tryGet(String path) {
    return new FetchedSecret().setFetchError(fetchingErrors.get(path)).setValue(encodedValuesMap.get(path));
  }

  public void applySecret(Supplier<String> fieldNameSupplier, Consumer<ClientErrorDescription> errorConsumer, Consumer<String> valueConsumer) {
    @NonNull var fieldName = fieldNameSupplier.get();
    if (fieldName != null && !fieldName.isEmpty()) {
      var fetchedSecret = tryGet(fieldName);
      Optional.ofNullable(fetchedSecret.getFetchError())
          .ifPresentOrElse(errorConsumer::accept, () -> {
            var value = fetchedSecret.getValue();
            if (value != null) {
              valueConsumer.accept(fetchedSecret.getValue());
            }
          });
    }
  }

  public String get(String path) {
    var secret = tryGet(path);
    if(secret.fetchError != null) {
      throw new ClientError(fetchingErrors.get(path));
    } else {
      return secret.value;
    }
  }
  
  @Data
  @Accessors(chain = true)
  public static class FetchedSecret {
    private String value;
    private ClientErrorDescription fetchError;
    
    public boolean containsError() {
      return fetchError != null;
    }
  }
  
  public static final ExternalProperties EMPTY = new ExternalProperties();
  
}
