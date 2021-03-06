package com.mentalbilisim.memapper.util.mapping;

import com.mentalbilisim.memapper.exception.TargetTypeInstantiationException;
import com.mentalbilisim.memapper.util.PrimitiveUtil;
import com.mentalbilisim.memapper.util.StringUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * author @er-han on 3/30/2017.
 */
public class MapByFieldNameUtil {

  private static final Logger logger = LoggerFactory.getLogger(MapByFieldNameUtil.class);

  /**
   * Maps given source object's suitable fields
   * to a newly instantiated object of type targetType.
   *
   * @param source     the object which's fields will be taken as map source.
   * @param targetType the type which will be used to instantiate a target object.
   * @param <SourceT>  source object' type.
   * @param <TargetT>  target object's type.
   * @return an object of type targetType.
   * @throws TargetTypeInstantiationException throws this when can't instantiate a new object.
   */
  public static <SourceT, TargetT> TargetT map(SourceT source,
                                               Class<TargetT> targetType)
      throws TargetTypeInstantiationException {

    if (source == null) {
      return null;
    }

    Class<?> sourceType = source.getClass();
    logger.debug("Started mapping from source type '"
        + sourceType.getName() + "' to target type '" + targetType.getName() + "'.");

    TargetT target;

    try {
      target = (TargetT) targetType.newInstance();
    } catch (Exception e) {
      TargetTypeInstantiationException exception =
          new TargetTypeInstantiationException(targetType, e);
      logger.debug(exception.getMessage());
      throw exception;
    }

    target = map(source, target);

    return target;
  }


  /**
   * Maps from source to target.
   * Both source and target must be Mappable.
   *
   * @param source    the object which's fields will be taken as map source.
   * @param target    the object which's fields will be taken as map target.
   * @param <SourceT> source object' type.
   * @param <TargetT> target object's type.
   * @return an object of type targetType.
   */
  public static <SourceT, TargetT> TargetT map(SourceT source,
                                               TargetT target) {
    if (source == null || target == null) {
      return null;
    }
    Class<?> sourceType = source.getClass();
    List<Field> sourceFields = CommonMapUtil.getAllFields(sourceType);

    logger.debug("Found fields in the source type: " + sourceFields);

    Class<?> targetType = target.getClass();
    List<Field> targetFields = CommonMapUtil.getAllFields(targetType);

    logger.debug("Found fields in the target type: " + targetFields);

    for (Field sourceField :
        sourceFields) {
      String sourceFieldName = sourceField.getName();
      Class<?> sourceFieldType = sourceField.getType();

      Stream<Field> targetFieldsStream = targetFields.stream();
      Optional<Field> targetFieldOptional = targetFieldsStream
          .filter(field ->
              field.getName() == sourceFieldName)
          .findFirst();

      if (!targetFieldOptional.isPresent()) {
        continue;
      }

      Class<?> targetFieldType = targetFieldOptional.get().getType();

      if (sourceFieldType.equals(targetFieldType)
          || (sourceFieldType.isPrimitive()
          && PrimitiveUtil.getWrapperClass(sourceFieldType).equals(targetFieldType))
          || (targetFieldType.isPrimitive()
          && PrimitiveUtil.getWrapperClass(targetFieldType).equals(sourceFieldType))) {

        String fieldNameCapitalized = StringUtil.capitalizeFirstLetter(sourceFieldName);


        Method sourceGetMethod;
        String sourceGetMethodName = "get" + fieldNameCapitalized;
        try {
          sourceGetMethod = sourceType.getMethod(sourceGetMethodName);
        } catch (NoSuchMethodException e) {
          logger.debug("Field '" + sourceType.getName() + " "
              + sourceFieldName + "' does not have a getter method.");
          continue;
        }

        Object sourceFieldVal;
        try {
          sourceFieldVal = sourceGetMethod.invoke(source, null);
        } catch (InvocationTargetException e) {
          logger.debug("Invokation of  '" + sourceType.getName()
              + " " + sourceGetMethodName + "' failed. "
              + "Probably it requires at least 1 arg.");
          continue;
        } catch (IllegalAccessException e) {
          logger.debug("Invokation of  '"
              + sourceType.getName() + " " + sourceGetMethodName + "' failed. "
              + "It has restricted access.");
          continue;
        } catch (IllegalArgumentException e) {
          logger.debug("Invokation of  '"
              + sourceType.getName() + " " + sourceGetMethodName + "' failed. "
              + "Illegal argument.");
          continue;
        }

        Method targetSetMethod;
        String targetSetMethodName = "set" + fieldNameCapitalized;
        try {
          targetSetMethod = targetType.getMethod(targetSetMethodName, targetFieldType);
        } catch (NoSuchMethodException e) {
          logger.debug("Field '" + targetType.getName() + " "
              + sourceFieldName + "' does not have a setter method.");
          continue;
        }


        try {
          targetSetMethod.invoke(target, sourceFieldVal);
        } catch (InvocationTargetException e) {
          logger.debug("Invokation of  '" + targetType.getName()
              + " " + targetSetMethodName + "' failed. "
              + "Probably it requires at least 1 arg.");
          continue;
        } catch (IllegalAccessException e) {
          logger.debug("Invokation of  '" + targetType.getName()
              + " " + targetSetMethodName + "' failed. "
              + "It has restricted access.");
          continue;
        } catch (IllegalArgumentException e) {
          logger.debug("Invokation of  '" + targetType.getName()
              + " " + targetSetMethodName + "' failed. "
              + "Illegal argument.");
          continue;
        }

      }
    }
    return target;
  }

  /**
   * Maps given source object's suitable fields
   * to a newly instantiated object of type targetType.
   *
   * @param source    the object which's fields will be taken as map source.
   * @param supplier  the supplier which will be used to instantiate a target object.
   * @param <SourceT> source object' type.
   * @param <TargetT> target object's type.
   * @return an object of type targetType.
   */
  public static <SourceT, TargetT> TargetT map(SourceT source,
                                               Supplier<TargetT> supplier) {
    if (supplier == null) {
      return null;
    }
    return map(source, supplier.get());
  }

  /**
   * @param sources    A list of object's to be mapped from.
   * @param targetType the type which will be used to instantiate a target object.
   * @param <SourceT>  source object' type.
   * @param <TargetT>  target object's type.
   * @return A List&lt;targetT&gt; object of type targetType.
   * @throws TargetTypeInstantiationException throws this when can't instantiate a new object.
   */
  public static <SourceT, TargetT> Iterable<TargetT> map(Iterable<SourceT> sources,
                                                         Class<TargetT> targetType)
      throws TargetTypeInstantiationException {

    try {
      TargetT targetT = targetType.newInstance();
    } catch (Exception e) {
      TargetTypeInstantiationException exception =
          new TargetTypeInstantiationException(targetType, e);
      logger.debug(exception.getMessage());
      throw exception;
    }

    return map(sources, () -> {
      try {
        return targetType.newInstance();
      } catch (Exception e) {
        return null;
      }
    });

  }


  /**
   * @param sources   A list of object's to be mapped from.
   * @param supplier  Target type's supplier function.
   * @param <SourceT> source object' type.
   * @param <TargetT> target object's type.
   * @return A List&lt;targetT&gt; object of type targetType.
   */
  public static <SourceT, TargetT> Iterable<TargetT> map(Iterable<SourceT> sources,
                                                         Supplier<TargetT> supplier) {
    if (sources == null || supplier == null) {
      return null;
    }

    List<TargetT> targets = new ArrayList<TargetT>();
    sources.forEach(source -> targets.add(map(source, supplier)));

    return targets;
  }
}
