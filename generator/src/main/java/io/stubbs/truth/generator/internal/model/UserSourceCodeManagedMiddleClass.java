package io.stubbs.truth.generator.internal.model;

import com.google.common.truth.Subject;
import io.stubbs.truth.generator.UserManagedSubject;
import io.stubbs.truth.generator.internal.TruthGeneratorRuntimeException;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;
import org.jboss.forge.roaster.model.source.AnnotationSource;
import org.jboss.forge.roaster.model.source.Import;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

import java.util.List;
import java.util.Optional;

import static io.stubbs.truth.generator.internal.Utils.msg;

/**
 * A {@link MiddleClass} which is provided from discovered source code (not a compiled class from class path or from
 * source code we generated). I.e. the main way users will supply their own {@link Subject}s into the grpah.
 *
 * @see UserManagedSubject
 */
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ToString
public class UserSourceCodeManagedMiddleClass<T> extends RoasterMiddleClass<T> implements MiddleClass<T> {

    public UserSourceCodeManagedMiddleClass(JavaClassSource sourceCodeModel, MethodSource factory) {
        super(sourceCodeModel, factory);
    }

    @Override
    public String getClassUnderTestSimpleName() {
        AnnotationSource<JavaClassSource> userManaged = super.sourceCodeModel.getAnnotation(UserManagedSubject.class);
        String underTest = userManaged.getStringValue();

        List<Import> imports = sourceCodeModel.getImports();
        String anObject = StringUtils.removeEnd(underTest, ".class");
        Optional<Import> first = imports.stream().filter(x -> x.getSimpleName().equals(anObject)).findFirst();

        String literalValue = userManaged.getLiteralValue();

        if (StringUtils.countMatches(literalValue, ".") > 1)
            return anObject;
        else if (first.isEmpty())
            throw new TruthGeneratorRuntimeException(msg("Cannot resolve type {}", literalValue));
        else
            return first.get().getQualifiedName();
    }
}
