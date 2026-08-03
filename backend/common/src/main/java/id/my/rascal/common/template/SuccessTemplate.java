package id.my.rascal.common.template;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SuccessTemplate<T>(
    boolean isSuccess,
    String message,
    T data,
    MetaTemplate meta
) { }
