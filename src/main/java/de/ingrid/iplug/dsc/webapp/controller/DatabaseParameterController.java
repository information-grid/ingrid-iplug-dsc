package de.ingrid.iplug.dsc.webapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;

import de.ingrid.admin.command.PlugdescriptionCommandObject;
import de.ingrid.admin.controller.AbstractController;
import de.ingrid.iplug.dsc.index.DatabaseConnection;
import de.ingrid.iplug.dsc.webapp.validation.DatabaseConnectionValidator;
import de.ingrid.utils.PlugDescription;

/**
 * Control the database parameter page.
 * 
 * @author joachim@wemove.com
 * 
 */
@Controller
@SessionAttributes("plugDescription")
public class DatabaseParameterController extends AbstractController {
    private final DatabaseConnectionValidator _validator;

    @Autowired
    public DatabaseParameterController(DatabaseConnectionValidator validator) {
        _validator = validator;
    }

    @RequestMapping(value = { "/iplug-pages/welcome.html",
            "/iplug-pages/dbParams.html" }, method = RequestMethod.GET)
    public String getParameters(
            final ModelMap modelMap,
            @ModelAttribute("plugDescription") final PlugdescriptionCommandObject commandObject) {

        DatabaseConnection dbConfig = (DatabaseConnection) commandObject
                .getConnection();
        // if no connection could be found, create a dummy object
        if (dbConfig == null) {
            dbConfig = new DatabaseConnection();
        }

        // write object into session
        modelMap.addAttribute("dbConfig", dbConfig);
        return AdminViews.DB_PARAMS;
    }

    @RequestMapping(value = "/iplug-pages/dbParams.html", method = RequestMethod.POST)
    public String post(
            @ModelAttribute("dbConfig") final DatabaseConnection commandObject,
            final BindingResult errors,
            @ModelAttribute("plugDescription") final PlugdescriptionCommandObject pdCommandObject) {

        // check if page contains any errors
        if (_validator.validateDBParams(errors).hasErrors()) {
            return AdminViews.DB_PARAMS;
        }

        // put values into plugdescription
        mapParamsToPD(commandObject, pdCommandObject);

        return AdminViews.SAVE;
    }

    private void mapParamsToPD(DatabaseConnection commandObject,
            PlugdescriptionCommandObject pdCommandObject) {

        pdCommandObject.setConnection(commandObject);

        pdCommandObject.setRankinTypes(true, false, false);

        // add required datatypes to PD
        //pdCommandObject.addDataType("IDF_1.0");
    }

    public boolean rankSupported(String rankType, String[] types) {
        for (String type : types) {
            if (type.contains(rankType))
                return true;
        }
        return false;
    }

}
