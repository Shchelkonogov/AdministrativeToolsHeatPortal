package ru.tecon.admTools.utils;

import jakarta.faces.context.FacesContext;
import org.postgresql.util.PSQLException;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.sql.SQLException;
import java.util.Set;

/**
 * @author Maksim Shchelkonogov
 * 19.05.2023
 */
public class AdmTools {

    private static final Set<String> sqlStates = Set.of("11111", "11112", "11122", "11102", "11103");

    @SuppressWarnings("unchecked")
    public static <T> T findBean(String beanName) {
        FacesContext context = FacesContext.getCurrentInstance();
        return (T) context.getApplication().evaluateExpressionGet(context, "#{" + beanName + "}", Object.class);
    }

    /**
     * Получение конечно объекта в случае записи через el через "."
     *
     * @param o начальный объект
     * @param field поле
     * @return ошибка выполнения
     * @throws ReflectiveOperationException ошибка выполнения
     * @throws IllegalArgumentException ошибка выполнения
     * @throws IntrospectionException ошибка выполнения
     */
    public static Object getPropertyValueViaReflection(Object o, String field)
            throws ReflectiveOperationException, IllegalArgumentException, IntrospectionException {
        Class<?> aClass = o.getClass();
        Object o1 = o;

        String[] split = field.split("[.]");
        for (String s: split) {
            o1 = new PropertyDescriptor(s, aClass).getReadMethod().invoke(o1);
            aClass = o1.getClass();
        }
        return o1;
    }

    /**
     * Вытаскиваю из {@link SQLException} сообщение.
     * @param ex Ошибка для проверки {@link SQLException}
     */
    public static String getSQLExceptionMessage(SQLException ex) {
        if ((ex.getSQLState() != null)
                && sqlStates.contains(ex.getSQLState())
                && (ex instanceof PSQLException)
                && (((PSQLException) ex).getServerErrorMessage() != null)) {
            return ((PSQLException) ex).getServerErrorMessage().getMessage();
        } else {
            return "Внутренняя ошибка сервера";
        }
    }
}
