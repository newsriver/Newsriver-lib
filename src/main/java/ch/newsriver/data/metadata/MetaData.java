package ch.newsriver.data.metadata;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.util.List;

/**
 * Created by eliapalme on 07/06/16.
 */


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ReadTime.class, name = "readTime"),
        @JsonSubTypes.Type(value = FinancialSentiment.class, name = "finSentiment"),
        @JsonSubTypes.Type(value = Category.class, name = "category")

})
public abstract class MetaData {

    private static final ObjectMapper mapper = new ObjectMapper();

    public String key() {

        AnnotationIntrospector annotationInspector = new JacksonAnnotationIntrospector();
        JavaType superClass = TypeFactory.defaultInstance().uncheckedSimpleType(MetaData.class);
        AnnotatedClass annotatedClass = AnnotatedClass.construct(superClass, mapper.getSerializationConfig());
        List<NamedType> subtypes = annotationInspector.findSubtypes(annotatedClass);

        for (NamedType type : subtypes) {
            if (type.getType().getName().equals(this.getClass().getName())) {
                return type.getName();
            }
        }
        return "";
    }

}
