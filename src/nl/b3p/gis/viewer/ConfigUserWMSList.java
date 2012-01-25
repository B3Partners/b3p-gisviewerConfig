package nl.b3p.gis.viewer;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.commons.struts.ExtendedMethodProperties;
import nl.b3p.gis.viewer.db.UserService;
import nl.b3p.gis.viewer.services.HibernateUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;
import org.hibernate.Session;

public class ConfigUserWMSList extends ViewerCrudAction {

    private static final Log log = LogFactory.getLog(ConfigUserWMSList.class);

    protected static final String MAPPING_COPY = "copy";

    @Override
    protected Map getActionMethodPropertiesMap() {
        Map map = super.getActionMethodPropertiesMap();

        ExtendedMethodProperties crudProp = null;

        crudProp = new ExtendedMethodProperties(MAPPING_COPY);
        crudProp.setDefaultForwardName(SUCCESS);
        crudProp.setDefaultMessageKey("message.applicatie.copy.success");
        crudProp.setAlternateForwardName(FAILURE);
        crudProp.setAlternateMessageKey("message.applicatie.copy.failed");
        map.put(MAPPING_COPY, crudProp);

        return map;
    }

    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return mapping.findForward(SUCCESS);
    }

    @Override
    protected void createLists(DynaValidatorForm form, HttpServletRequest request) throws Exception {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List<UserService> services = sess.createQuery("from UserService where"
                + " use_in_list = :uselist order by name")
                .setParameter("uselist", true)
                .list();

        if (services != null && services.size() > 0) {
            request.setAttribute("services", services);
        }
    }

    @Override
    public ActionForward save(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (!isTokenValid(request)) {
            addAlternateMessage(mapping, request, TOKEN_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        /* Applicatie ophalen en opslaan */
        UserService service = getService(dynaForm, request); 

        /* Als bij het opslaan app null is dan wordt er een nieuwe applicatie
         opgeslagen. Alleen-lezen is voor een nieuw app object al op true gezet */
        if (service == null) {
            service = new UserService();            
            populateObject(dynaForm, service, request);
        } else {
            populateObject(dynaForm, service, request);
        }

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        sess.saveOrUpdate(service);
        sess.flush();
        
        /* Klaarzetten form */
        populateForm(service, dynaForm, request);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return mapping.findForward(SUCCESS);
    }

    @Override
    public ActionForward edit(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
                
        /* Applicatie ophalen en klaarzetten voor form */
        UserService service = getService(dynaForm, request);
        populateForm(service, dynaForm, request);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return getDefaultForward(mapping, request);
    }

    @Override
    public ActionForward delete(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (!isTokenValid(request)) {
            addAlternateMessage(mapping, request, TOKEN_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        /* Applicatie ophalen en verwijderen */
        UserService service = getService(dynaForm, request);

        if (service != null) {
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            sess.delete(service);
            sess.flush();
        }

        dynaForm.initialize(mapping);

        prepareMethod(dynaForm, request, LIST, EDIT);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return getDefaultForward(mapping, request);
    }
    
    private UserService getService(DynaValidatorForm dynaForm, HttpServletRequest request) {
        Integer id = (Integer) dynaForm.get("serviceId");
        
        if (id == null || id < 1) {
            return null;
        }

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        UserService service = (UserService) sess.get(UserService.class, id);

        return service;
    }

    private void populateForm(UserService service, DynaValidatorForm dynaForm, HttpServletRequest request) {
        if (service == null) {
            return;
        }

        dynaForm.set("serviceId", service.getId());
        dynaForm.set("name", service.getName());
        dynaForm.set("groupname", service.getGroupname());
        dynaForm.set("url", service.getUrl());
        dynaForm.set("sld_url", service.getSld_url());
    }

    private void populateObject(DynaValidatorForm dynaForm, UserService service,
            HttpServletRequest request) {
        
        Integer id = (Integer) dynaForm.get("serviceId");

        if (id != null && id != 0) {
            service.setId(id);
        }
        
        service.setCode("");
        service.setUse_in_list(true);
        service.setName(FormUtils.nullIfEmpty(dynaForm.getString("name")));
        service.setGroupname(FormUtils.nullIfEmpty(dynaForm.getString("groupname")));
        service.setUrl(FormUtils.nullIfEmpty(dynaForm.getString("url")));
        service.setSld_url(FormUtils.nullIfEmpty(dynaForm.getString("sld_url")));
    }
}
