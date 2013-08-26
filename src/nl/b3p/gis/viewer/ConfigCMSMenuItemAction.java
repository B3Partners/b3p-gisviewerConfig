package nl.b3p.gis.viewer;

import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.services.FormUtils;
import static nl.b3p.gis.viewer.ViewerCrudAction.ACKNOWLEDGE_MESSAGES;
import nl.b3p.gis.viewer.db.CMSMenuItem;
import nl.b3p.gis.viewer.services.HibernateUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;
import org.hibernate.Session;

/**
 *
 * @author Boy de Wit
 */
public class ConfigCMSMenuItemAction extends ViewerCrudAction {

    private static final Log logger = LogFactory.getLog(ConfigCMSMenuItemAction.class);
    
    private CMSMenuItem getCMSMenuItem(DynaValidatorForm form, boolean createNew) {
        if (createNew) {
            return new CMSMenuItem();
        }

        Integer id = (Integer) form.get("cmsMenuItemID");

        if (id != null && id > 0) {
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            CMSMenuItem cmsMenuItem = (CMSMenuItem) sess.get(CMSMenuItem.class, id);

            if (cmsMenuItem != null) {
                return cmsMenuItem;
            }
        }

        return null;
    }

    private CMSMenuItem getFirstCMSMenuItem() {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List lijst = sess.createQuery("from CMSMenuItem order by cdate")
                .setMaxResults(1).list();

        if (lijst != null && lijst.size() == 1) {
            return (CMSMenuItem) lijst.get(0);
        }

        return null;
    }

    @Override
    protected void createLists(DynaValidatorForm form, HttpServletRequest request) throws Exception {

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List cmsMenuItems = sess.createQuery("from CMSMenuItem order by cdate").list();
        request.setAttribute("cmsMenuItems", cmsMenuItems);
    }

    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CMSMenuItem cmsMenuItem = getCMSMenuItem(dynaForm, false);
        
        if (cmsMenuItem == null) {
            cmsMenuItem = getFirstCMSMenuItem();
        }

        populateCMSMenuItemForm(cmsMenuItem, dynaForm, request);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return mapping.findForward(SUCCESS);
    }

    @Override
    public ActionForward edit(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CMSMenuItem cmsMenuItem = getCMSMenuItem(dynaForm, false);
        if (cmsMenuItem == null) {
            cmsMenuItem = getFirstCMSMenuItem();
        }

        populateCMSMenuItemForm(cmsMenuItem, dynaForm, request);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return getDefaultForward(mapping, request);
    }

    @Override
    public ActionForward save(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (!isTokenValid(request)) {
            prepareMethod(dynaForm, request, EDIT, LIST);
            addAlternateMessage(mapping, request, TOKEN_ERROR_KEY);

            return getAlternateForward(mapping, request);
        }

        // nieuwe default actie op delete zetten
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        ActionErrors errors = dynaForm.validate(mapping, request);
        if (!errors.isEmpty()) {
            addMessages(request, errors);
            prepareMethod(dynaForm, request, EDIT, LIST);
            addAlternateMessage(mapping, request, VALIDATION_ERROR_KEY);

            return getAlternateForward(mapping, request);
        }

        CMSMenuItem cmsMenuItem = getCMSMenuItem(dynaForm, true);

        if (cmsMenuItem == null) {
            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, NOTFOUND_ERROR_KEY);

            return getAlternateForward(mapping, request);
        }

        populateCMSMenuItemObject(dynaForm, cmsMenuItem, request);

        sess.saveOrUpdate(cmsMenuItem);
        sess.flush();
        
        populateCMSMenuItemForm(cmsMenuItem, dynaForm, request);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        
        return mapping.findForward(SUCCESS);
    }

    @Override
    public ActionForward delete(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (!isTokenValid(request)) {
            prepareMethod(dynaForm, request, EDIT, LIST);
            addAlternateMessage(mapping, request, TOKEN_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        /* Probeer Tekstblok uit de database te verwijderen */
        Session sess = null;
        CMSMenuItem cmsMenuItem = null;

        try {
            sess = HibernateUtil.getSessionFactory().getCurrentSession();
            cmsMenuItem = getCMSMenuItem(dynaForm, false);

            if (cmsMenuItem == null) {
                prepareMethod(dynaForm, request, LIST, EDIT);
                addAlternateMessage(mapping, request, NOTFOUND_ERROR_KEY);

                return getAlternateForward(mapping, request);
            }

            sess.delete(cmsMenuItem);
            sess.flush();

        } catch (Exception ex) {
            logger.error("Fout tijdens verwijderen van cms menu " + cmsMenuItem.getTitel(), ex);

            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, GENERAL_ERROR_KEY);

            return getAlternateForward(mapping, request);
        }

        CMSMenuItem menuItem = getFirstCMSMenuItem();

        if (menuItem != null) {
            populateCMSMenuItemForm(menuItem, dynaForm, request);
        } else {
            dynaForm.initialize(mapping);
        }
        
        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return mapping.findForward(SUCCESS);
    }

    private void populateCMSMenuItemForm(CMSMenuItem cmsMenuItem, DynaValidatorForm dynaForm, HttpServletRequest request) {
        if (cmsMenuItem == null) {
            return;
        }

        dynaForm.set("cmsMenuItemID", cmsMenuItem.getId());
        dynaForm.set("titel", cmsMenuItem.getTitel());
        dynaForm.set("url", cmsMenuItem.getUrl());
        dynaForm.set("icon", cmsMenuItem.getIcon());
        dynaForm.set("volgordenr", cmsMenuItem.getVolgordenr());
    }

    private void populateCMSMenuItemObject(DynaValidatorForm dynaForm, CMSMenuItem cmsMenuItem, HttpServletRequest request) {
        Integer id = (Integer) dynaForm.get("cmsMenuItemID");

        if (id != null && id != 0) {
            cmsMenuItem.setId(id);
        }

        cmsMenuItem.setTitel(FormUtils.nullIfEmpty(dynaForm.getString("titel")));
        cmsMenuItem.setUrl(FormUtils.nullIfEmpty(dynaForm.getString("url")));
        cmsMenuItem.setIcon(FormUtils.nullIfEmpty(dynaForm.getString("icon")));
        cmsMenuItem.setVolgordenr((Integer)dynaForm.get("volgordenr"));
        cmsMenuItem.setCdate(new Date());
    }
}