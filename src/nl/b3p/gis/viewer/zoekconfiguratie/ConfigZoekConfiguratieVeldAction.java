package nl.b3p.gis.viewer.zoekconfiguratie;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.gis.viewer.ConfigZoekConfiguratieAction;
import nl.b3p.gis.viewer.ViewerCrudAction;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.zoeker.configuratie.Attribuut;
import nl.b3p.zoeker.configuratie.ResultaatAttribuut;
import nl.b3p.zoeker.configuratie.ZoekAttribuut;
import nl.b3p.zoeker.configuratie.ZoekConfiguratie;
import nl.b3p.zoeker.services.Zoeker;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;
import org.geotools.data.DataStore;
import org.hibernate.Session;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

public class ConfigZoekConfiguratieVeldAction extends ViewerCrudAction {

    private static final Log logger = LogFactory.getLog(ConfigZoekConfiguratieAction.class);
    private static final String ZOEKATTRIBUUTID = "zoekAttribuutId";
    private static final String RESULTAATATTRIBUUTID = "resultaatAttribuutId";
    //private static final String ZOEKCONFIGURATIEID = "zoekConfiguratieId";
    private static final String ATTRIBUUTTYPE = "attribuutType";

    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Attribuut attr = getAttribuut(request,false);
        if (attr != null) {
            populateForm(attr, dynaForm);
            
            if (attr instanceof ResultaatAttribuut) {
                request.setAttribute(ATTRIBUUTTYPE,"resultaat");
            } else {
                request.setAttribute(ATTRIBUUTTYPE,"zoek");
            }

            if (attr.getType() != null) {
                request.setAttribute("selType", attr.getType().toString());
            }

        } else {
            
            //als een attr null is dan is het een nieuw. Geef het ZoekConfigId en attribuutType door
            request.setAttribute(ATTRIBUUTTYPE,request.getParameter(ATTRIBUUTTYPE));
            request.setAttribute(ConfigZoekConfiguratieAction.ZOEKCONFIGURATIEID,request.getParameter(ConfigZoekConfiguratieAction.ZOEKCONFIGURATIEID));
        }
        createLists(request);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return mapping.findForward(SUCCESS);
    }

    @Override
    public ActionForward save(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Attribuut attr=populateObject(dynaForm,request);
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        sess.saveOrUpdate(attr);
        sess.flush();        
        request.setAttribute("doClose", true);
        prepareMethod(dynaForm, request, LIST, EDIT);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
    }

    public void createLists(HttpServletRequest request) throws IOException, Exception {
        Attribuut a = getAttribuut(request,false);
        ZoekConfiguratie zc=null;

        if (a!=null) {
            zc=a.getZoekConfiguratie();
        }

        /*Als zc nog steeds null is dan is het attribuut waarschijnlijk nieuw
         en staat het id van de zoekconfiguratie op het request*/
        if (zc == null) {
            zc=getZoekConfiguratie(request);
        }

        DataStore ds = Zoeker.getDataStore(zc.getBron());
        String ftype = zc.getFeatureType();
        SimpleFeatureType sft = null;

        try {
            sft = ds.getSchema(ftype);
        } catch (NullPointerException ex) {
            logger.error("NullPointerException bij ophalen schema van datastore: ");
        } finally {
            if (ds != null) {
                ds.dispose();
            }
        }

        if (sft != null) {
            List<AttributeDescriptor> descriptors = sft.getAttributeDescriptors();
            List attributen = new ArrayList();
            //maak een lijst met mogelijke attributen en de binding class namen.
            for (int i = 0; i < descriptors.size(); i++) {
                String[] attr = new String[2];
                attr[0] = descriptors.get(i).getName().toString();
                attr[1]="";
                if (descriptors.get(i).getType().getBinding()!=null){
                    String type=descriptors.get(i).getType().getBinding().getName();
                    type=type.substring(type.lastIndexOf(".")+1);
                    attr[1]=type;
                }
                attributen.add(attr);

            }

            request.setAttribute("attribuutNamen", attributen);
        }

        Session sess =  HibernateUtil.getSessionFactory().getCurrentSession();
        List zoekConfigs = sess.createQuery("from ZoekConfiguratie").list();
        request.setAttribute("inputZoekConfigList", zoekConfigs);
    }
    
    private Attribuut populateObject(DynaValidatorForm dynaForm,HttpServletRequest request){
        Attribuut attr = getAttribuut(request, true);
        attr.setAttribuutnaam(dynaForm.getString("attribuutnaam"));
        attr.setLabel(dynaForm.getString("label"));
        attr.setNaam(dynaForm.getString("naam"));

        if (FormUtils.nullIfEmpty(dynaForm.getString("type"))!=null) {
            attr.setType(new Integer(dynaForm.getString("type")));
        }

        if (FormUtils.nullIfEmpty(dynaForm.getString("volgorde"))!=null) {
            attr.setVolgorde(new Integer(dynaForm.getString("volgorde")));
        }

        if (FormUtils.nullIfEmpty(dynaForm.getString("inputtype"))!=null) {
            if (attr instanceof ZoekAttribuut) {
                ((ZoekAttribuut)attr).setInputtype(new Integer(dynaForm.getString("inputtype")));
            }
        }

        if (FormUtils.nullIfEmpty(dynaForm.getString("inputsize"))!=null) {
            if (attr instanceof ZoekAttribuut) {
                ((ZoekAttribuut)attr).setInputsize(new Integer(dynaForm.getString("inputsize")));
            }
        }

        if (FormUtils.nullIfEmpty(dynaForm.getString("inputzoekconfiguratie")) != null) {
            if (attr instanceof ZoekAttribuut) {
                Integer inputZoekConfigId = new Integer(dynaForm.getString("inputzoekconfiguratie"));
                Session sess =  HibernateUtil.getSessionFactory().getCurrentSession();
                List zoekConfigs = sess.createQuery("from ZoekConfiguratie where id = :id")
                        .setParameter("id", inputZoekConfigId)
                        .list();
                if (zoekConfigs.size() == 1) {
                    ((ZoekAttribuut)attr).setInputzoekconfiguratie((ZoekConfiguratie)zoekConfigs.get(0));
                }
            }
        }

        if (FormUtils.nullIfEmpty(dynaForm.getString("inputzoekconfiguratie")) == null) {
            if (attr instanceof ZoekAttribuut) {
                ((ZoekAttribuut)attr).setInputzoekconfiguratie(null);
            }
        }

        //zet de zoekconfiguratie als het object nieuw is
        if (attr.getId()==null){
            ZoekConfiguratie zc= getZoekConfiguratie(request);
            attr.setZoekConfiguratie(zc);
        }
        
        attr.setOmschrijving(FormUtils.nullIfEmpty(dynaForm.getString("omschrijving")));

        return attr;

    }
    private void populateForm(Attribuut a, DynaValidatorForm dynaForm) {
        if (a instanceof ZoekAttribuut) {
            dynaForm.set("zoekAttribuutId", a.getId().toString());
        } else if (a instanceof ResultaatAttribuut) {
            dynaForm.set("resultaatAttribuutId", a.getId().toString());
        }

        dynaForm.set("label", a.getLabel());
        dynaForm.set("naam", a.getNaam());

        if (a.getType() != null) {
            dynaForm.set("type", a.getType().toString());
        }

        if (a.getVolgorde() != null) {
            dynaForm.set("volgorde", a.getVolgorde().toString());
        }

        if (a instanceof ZoekAttribuut) {
            ZoekAttribuut za = (ZoekAttribuut)a;

            Integer inputtype = za.getInputtype();
            Integer inputsize = za.getInputsize();
            ZoekConfiguratie zc = za.getInputzoekconfiguratie();

            if (inputtype != null) {
                dynaForm.set("inputtype", inputtype.toString());
            }

            if (inputsize != null) {
                dynaForm.set("inputsize", inputsize.toString());
            }

            if (zc != null) {
                dynaForm.set("inputzoekconfiguratie", zc.getId().toString());
            }
        }

        dynaForm.set("attribuutnaam", a.getAttribuutnaam());
        
        if (a.getOmschrijving() != null) {
            dynaForm.set("omschrijving", a.getOmschrijving());
        }
    }

    private Attribuut getAttribuut(HttpServletRequest request,boolean createNew) {
        Attribuut attribuut = null;
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        if (FormUtils.nullIfEmpty(request.getParameter(ZOEKATTRIBUUTID)) != null) {
            Integer id = new Integer(request.getParameter(ZOEKATTRIBUUTID));
            attribuut = (ZoekAttribuut) sess.get(ZoekAttribuut.class, id);
        } else if (FormUtils.nullIfEmpty(request.getParameter(RESULTAATATTRIBUUTID)) != null) {
            Integer id = new Integer(request.getParameter(RESULTAATATTRIBUUTID));
            attribuut = (ResultaatAttribuut) sess.get(ResultaatAttribuut.class, id);
        }else if(createNew){
            if ("zoek".equalsIgnoreCase(request.getParameter(ATTRIBUUTTYPE))){
                return new ZoekAttribuut();
            }else{
                return new ResultaatAttribuut();
            }
        }
        return attribuut;
    }
    private ZoekConfiguratie getZoekConfiguratie(HttpServletRequest request){
        String zid=request.getParameter(ConfigZoekConfiguratieAction.ZOEKCONFIGURATIEID);
        if (zid!=null){
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            ZoekConfiguratie zc=(ZoekConfiguratie) sess.get(ZoekConfiguratie.class,new Integer(zid));
            return zc;
        }
        return null;
    }

}
