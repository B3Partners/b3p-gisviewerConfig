package nl.b3p.gis.viewer;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.services.FormUtils;
import static nl.b3p.gis.viewer.ViewerCrudAction.ACKNOWLEDGE_MESSAGES;
import nl.b3p.gis.viewer.db.CMSMenu;
import nl.b3p.gis.viewer.db.CMSMenuItem;
import nl.b3p.gis.viewer.db.Themas;
import nl.b3p.gis.viewer.db.ZoekconfiguratieThemas;
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
public class ConfigCMSMenuAction extends ViewerCrudAction {

    private static final Log logger = LogFactory.getLog(ConfigCMSMenuAction.class);

    private CMSMenu getCMSMenu(DynaValidatorForm form, boolean createNew) {
        if (createNew) {
            return new CMSMenu();
        }

        Integer id = (Integer) form.get("cmsMenuID");

        if (id != null && id > 0) {
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            CMSMenu cmsMenu = (CMSMenu) sess.get(CMSMenu.class, id);

            if (cmsMenu != null) {
                return cmsMenu;
            }
        }

        return null;
    }

    private CMSMenu getFirstCMSMenu() {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List lijst = sess.createQuery("from CMSMenu order by cdate")
                .setMaxResults(1).list();

        if (lijst != null && lijst.size() == 1) {
            return (CMSMenu) lijst.get(0);
        }

        return null;
    }

    @Override
    protected void createLists(DynaValidatorForm form, HttpServletRequest request) throws Exception {

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List cmsMenus = sess.createQuery("from CMSMenu order by cdate").list();
        request.setAttribute("cmsMenus", cmsMenus);
        
        /* Klaarzetten alle menu items */
        request.setAttribute("cmsMenuItems", getAllCMSMenuItems());
        
        
    }

    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CMSMenu cmsMenu = getCMSMenu(dynaForm, false);

        if (cmsMenu == null) {
            cmsMenu = getFirstCMSMenu();
        }

        populateCMSMenuForm(cmsMenu, dynaForm, request);        

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return mapping.findForward(SUCCESS);
    }

    @Override
    public ActionForward edit(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CMSMenu cmsMenu = getCMSMenu(dynaForm, false);
        if (cmsMenu == null) {
            cmsMenu = getFirstCMSMenu();
        }

        populateCMSMenuForm(cmsMenu, dynaForm, request);

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

        CMSMenu cmsMenu = getCMSMenu(dynaForm, true);

        if (cmsMenu == null) {
            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, NOTFOUND_ERROR_KEY);

            return getAlternateForward(mapping, request);
        }

        populateCMSMenuObject(dynaForm, cmsMenu, request);

        sess.saveOrUpdate(cmsMenu);
        sess.flush();
        
        /* Menu Items opslaan */
        String[] cmsMenuItemsAan = dynaForm.getStrings("cmsMenuItemsAan");

        for (int i = 0; i < cmsMenuItemsAan.length; i++) {
            String cmsMenuItemId = cmsMenuItemsAan[i];
            CMSMenuItem item = (CMSMenuItem) sess.get(CMSMenuItem.class, new Integer(cmsMenuItemId));     
            
            cmsMenu.addMenuItem(item);
        }
        
        sess.save(cmsMenu);       
        sess.flush();
        
        populateCMSMenuForm(cmsMenu, dynaForm, request);

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
        CMSMenu cmsMenu = null;

        try {
            sess = HibernateUtil.getSessionFactory().getCurrentSession();
            cmsMenu = getCMSMenu(dynaForm, false);

            if (cmsMenu == null) {
                prepareMethod(dynaForm, request, LIST, EDIT);
                addAlternateMessage(mapping, request, NOTFOUND_ERROR_KEY);

                return getAlternateForward(mapping, request);
            }

            sess.delete(cmsMenu);
            sess.flush();

        } catch (Exception ex) {
            logger.error("Fout tijdens verwijderen van cms menu " + cmsMenu.getTitel(), ex);

            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, GENERAL_ERROR_KEY);

            return getAlternateForward(mapping, request);
        }

        CMSMenu menu = getFirstCMSMenu();

        if (menu != null) {
            populateCMSMenuForm(menu, dynaForm, request);
        } else {
            dynaForm.initialize(mapping);
        }

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return mapping.findForward(SUCCESS);
    }

    private void populateCMSMenuForm(CMSMenu cmsMenu, DynaValidatorForm dynaForm, HttpServletRequest request) {
        if (cmsMenu == null) {
            return;
        }

        Integer menuId = cmsMenu.getId();
        dynaForm.set("cmsMenuID", menuId);
        dynaForm.set("titel", cmsMenu.getTitel());

        /* Klaarzetten menu items */
        request.setAttribute("cmsMenuItems", getAllCMSMenuItems());
        
        List<String> cmsMenuItemsAan = new ArrayList<String>();
        List<CMSMenuItem> currentItems = getCurrentCMSMenuItems(menuId);
        
        for (CMSMenuItem menuItem : getAllCMSMenuItems()) {
            
            if (currentItems.contains(menuItem)) {
                cmsMenuItemsAan.add(menuItem.getId().toString());
            }
        }
        
        dynaForm.set("cmsMenuItemsAan", cmsMenuItemsAan.toArray(new String[cmsMenuItemsAan.size()]));
    }
    
    private List<CMSMenuItem> getAllCMSMenuItems() {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        return sess.createQuery("from CMSMenuItem order by volgordenr").list();      
    }

    private void populateCMSMenuObject(DynaValidatorForm dynaForm, CMSMenu cmsMenu, HttpServletRequest request) {
        Integer id = (Integer) dynaForm.get("cmsMenuID");

        if (id != null && id != 0) {
            cmsMenu.setId(id);
        }

        cmsMenu.setTitel(FormUtils.nullIfEmpty(dynaForm.getString("titel")));
        cmsMenu.setCdate(new Date());
    }
    
    private List<CMSMenuItem> getCurrentCMSMenuItems(Integer menuId) {
        List<CMSMenuItem> menuItems = new ArrayList<CMSMenuItem>();
        
        if (menuId == null || menuId < 1) {
            return menuItems;
        }

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        menuItems = sess.createQuery("select item from CMSMenuItem item"
                + " where item.id in (select cmsMenuItems.id"
                + " from CMSMenu menu inner join menu.cmsMenuItems cmsMenuItems"
                + " where menu.id = :menuId) order by item.volgordenr DESC")
                .setParameter("menuId", menuId)
                .list();
       
        return menuItems;
    }
}