package ru.tecon.admTools.utils;

import org.postgresql.util.PSQLException;

import javax.faces.context.FacesContext;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.sql.SQLException;

/**
 * @author Maksim Shchelkonogov
 * 19.05.2023
 */
public class AdmTools {

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
        if ((ex.getSQLState() != null) &&
                ex.getSQLState().equals("11111") &&
                (ex instanceof PSQLException) &&
                (((PSQLException) ex).getServerErrorMessage() != null)) {
                return ((PSQLException) ex).getServerErrorMessage().getMessage();
        } else {
            return "Внутренняя ошибка сервера";
        }
    }
}
