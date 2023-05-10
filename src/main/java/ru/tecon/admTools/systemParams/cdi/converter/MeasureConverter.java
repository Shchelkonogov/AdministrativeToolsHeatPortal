package ru.tecon.admTools.systemParams.cdi.converter;

import ru.tecon.admTools.systemParams.cdi.scope.application.MeasureController;
import ru.tecon.admTools.systemParams.model.Measure;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 * Конвертер для единиц измерений свойств структур
 * @author Maksim Shchelkonogov
 */
@FacesConverter("measureConverter")
public class MeasureConverter implements Converter {

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        ValueExpression vex = context.getApplication().getExpressionFactory()
                .createValueExpression(context.getELContext(), "#{measureControllerApplication}", MeasureController.class);

        MeasureController divisions = (MeasureController) vex.getValue(context.getELContext());

        return divisions.getMeasures().stream()
                .filter(measure -> measure.getId() == Integer.parseInt(value))
                .findFirst()
                .orElse(null);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        return String.valueOf(((Measure) value).getId());
    }
}
