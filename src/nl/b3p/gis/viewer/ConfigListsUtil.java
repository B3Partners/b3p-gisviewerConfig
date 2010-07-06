/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.gis.viewer;

import nl.b3p.gis.geotools.DataStoreUtil;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import nl.b3p.gis.viewer.db.Connecties;
import nl.b3p.gis.viewer.services.GisPrincipal;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.zoeker.configuratie.Bron;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;
import org.geotools.data.DataStore;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

/**
 *
 * @author Roy
 */
public class ConfigListsUtil {

    private static final Log log = LogFactory.getLog(ConfigListsUtil.class);

    private static Bron getBron(Session sess, Integer bronId) {
        WebContext ctx = WebContextFactory.get();
        HttpServletRequest request = ctx.getHttpServletRequest();
        GisPrincipal user = GisPrincipal.getGisPrincipal(request);
        return getBron(sess, user, bronId);
    }

    public static Bron getBron(Session sess, GisPrincipal user, Integer bronId) {
        Bron b = null;
        if (bronId == null || bronId.intValue() == 0) {
            b = user.getKbWfsConnectie();
        } else if (bronId.intValue() > 0) {
            b = (Bron) sess.get(Bron.class, bronId);
        }
        return b;
    }

    public static List getPossibleFeaturesById(Integer connId) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        try {
            sess.beginTransaction();
            Bron b = getBron(sess, connId);
            return getPossibleFeatures(b);
        } catch (Exception e) {
            log.error("getPossibleFeaturesById error: ", e);
        } finally {
            sess.close();
        }
        return null;
    }

    public static List getPossibleAttributesById(Integer connId, String feature) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        try {
            sess.beginTransaction();
            Bron b = getBron(sess, connId);
            if (b == null) {
                return null;
            }            
            return getPossibleAttributes(b, feature);
        } catch (Exception e) {
            log.error("getPossibleAttributesById error: ", e);
        } finally {
            sess.close();
        }
        return null;
    }

    /**
     * Maakt een lijst met mogelijke features voor de gegeven wfs connectie en gebruiker
     */
    public static List getPossibleFeatures(Bron b) throws Exception {
        if (b == null) {
            return null;
        }
        ArrayList returnValue = null;
        DataStore ds= b.toDatastore();
        try{
            String[] features=ds.getTypeNames();
            if (features != null) {
                returnValue = new ArrayList();
                for (int i = 0; i < features.length; i++) {
                    String[] s = new String[2];
                    s[0] = BaseGisAction.removeNamespace(features[i]);
                    s[1] = BaseGisAction.removeNamespace(features[i]);
                    returnValue.add(s);
                }
            }
        }finally{
            ds.dispose();
        }
        return returnValue;
    }
    /**
     * Maakt een lijst met mogelijke attributen van een meegegeven featureType.
     */
    public static List getPossibleAttributes(Bron b, String type) throws Exception {
        if (b == null || type == null) {
            return null;
        }
        ArrayList returnValue = new ArrayList();
        DataStore ds=b.toDatastore();
        returnValue=DataStoreUtil.getAttributeNames(ds, type);
        returnValue = new ArrayList();
        try{
            SimpleFeatureType sft=ds.getSchema(type);
            List<AttributeDescriptor> attributes=sft.getAttributeDescriptors();
            Iterator<AttributeDescriptor> it= attributes.iterator();
            while(it.hasNext()){
                AttributeDescriptor attribute=it.next();
                String[] s = new String[2];
                s[0] = attribute.getName().toString();
                s[1] = attribute.getLocalName();
                returnValue.add(s);
            }
        }finally{
            ds.dispose();
        }
        return returnValue;
    }    
}
