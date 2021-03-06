package Utils.Parser;

import Beans.ControllerBean;
import Beans.PMBean;
import Database.Configure.Configuration;
import Database.Tuples.ControlPlaneTuple;
import Database.Tuples.MastershipTuple;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import org.projectfloodlight.openflow.protocol.OFType;

import java.util.ArrayList;
import java.util.HashMap;

public class JsonParser extends AbstractParser implements Parser {
    public JsonParser() {
        parserName = parserType.JSON;
    }

    public void parseAndMakeConfiguration (String rawJsonString) {
        if (rawJsonString == null) {
            throw new NullPointerException();
        }

        Configuration config = Configuration.getInstance();

        JsonObject parser = JsonObject.readFrom(rawJsonString);
        JsonArray controllersArray = parser.get("Controllers").asArray();
        JsonArray pmsArray = parser.get("PMs").asArray();
        JsonArray relationshipArray = parser.get("Relationships").asArray();

        // For Controllers
        for (int index = 0; index < controllersArray.size(); index++) {
            JsonObject tmpObj = controllersArray.get(index).asObject();

            ControllerBean tmpControllerBean = new ControllerBean(tmpObj.get("controllerId").asString());
            tmpControllerBean.setName(tmpObj.get("name").asString());
            tmpControllerBean.setIpAddr(tmpObj.get("ipAddr").asString());
            tmpControllerBean.setRestPort(tmpObj.get("restPort").asString());
            tmpControllerBean.setSshPort(tmpObj.get("sshPort").asString());
            tmpControllerBean.setControllerGuiId(tmpObj.get("controllerGuiId").asString());
            tmpControllerBean.setControllerGuiPw(tmpObj.get("controllerGuiPw").asString());
            tmpControllerBean.setSshId(tmpObj.get("sshId").asString());
            tmpControllerBean.setSshPw(tmpObj.get("sshPw").asString());
            tmpControllerBean.setSshRootId(tmpObj.get("sshRootId").asString());
            tmpControllerBean.setSshRootPw(tmpObj.get("sshRootPw").asString());
            tmpControllerBean.setMaxCPUs(Integer.valueOf(tmpObj.get("numMaxCPU").asString()));
            tmpControllerBean.setMinCPUs(Integer.valueOf(tmpObj.get("numMinCPU").asString()));
            tmpControllerBean.setNumCPUs(Integer.valueOf(tmpObj.get("numMaxCPU").asString()));
            tmpControllerBean.setCpuBitmap(new int[tmpControllerBean.getMaxCPUs()]);

            config.getControllers().add(tmpControllerBean);
        }

        // For PMs
        for (int index = 0; index < pmsArray.size(); index++) {
            JsonObject tmpObj = pmsArray.get(index).asObject();

            PMBean tmpPMBean = new PMBean(tmpObj.get("ipAddr").asString());
            tmpPMBean.setSshPort(tmpObj.get("sshPort").asString());
            tmpPMBean.setSshId(tmpObj.get("sshId").asString());
            tmpPMBean.setSshPw(tmpObj.get("sshPw").asString());
            tmpPMBean.setSshRootId(tmpObj.get("sshRootId").asString());
            tmpPMBean.setSshRootPw(tmpObj.get("sshRootPw").asString());

            config.getPms().add(tmpPMBean);
        }

        // For Relationships
        for (int index1 = 0; index1 < relationshipArray.size(); index1++) {
            JsonObject tmpObj = relationshipArray.get(index1).asObject();
            String tmpPmIpAddr = tmpObj.get("PMIpAddr").asString();

            PMBean tmpPMBean = config.getPMBean(tmpPmIpAddr);
            config.getRelationships().putIfAbsent(tmpPMBean, new ArrayList<>());

            JsonArray tmpControllers = tmpObj.get("Controllers").asArray();
            for (int index2 = 0; index2 < tmpControllers.size(); index2++) {
                JsonObject tmpControllerObj = tmpControllers.get(index2).asObject();
                String tmpControllerName = tmpControllerObj.get("name").asString();
                String tmpControllerId = tmpControllerObj.get("controllerId").asString();

                ControllerBean tmpControllerBean = config.getControllerBean(tmpControllerName);
                config.getRelationships().get(tmpPMBean).add(tmpControllerBean);
            }
        }
    }

    public MastershipTuple parseMastershipMonitoringResults(String rawResults) {
        MastershipTuple result = new MastershipTuple();

        JsonObject parser = JsonObject.readFrom(rawResults);
        JsonArray switches = parser.get("deviceIds").asArray();
        for (int index = 0; index < switches.size(); index++) {
            result.addSwitch(switches.get(index).asString());
        }

        return result;
    }

    public HashMap<String, ControlPlaneTuple> parseControlPlaneMonitoringResult(ControllerBean controller, String rawResult) {
        HashMap<String, ControlPlaneTuple> results = new HashMap<>();

        if(rawResult == null || rawResult == "") {
            throw new NullPointerException();
        }

        JsonObject parser = JsonObject.readFrom(rawResult);
        JsonArray switches = parser.get("devices").asArray();

        for (int index = 0; index < switches.size(); index++) {
            JsonObject elemSwitch = switches.get(index).asObject();
            String tmpDpid = elemSwitch.get("id").asString();

            if (results.containsKey(tmpDpid)) {
                throw new ControlPlaneMonitoringSanityException();
            }

            results.put(tmpDpid, new ControlPlaneTuple());
            ControlPlaneTuple resultTuple = results.get(tmpDpid);

            resultTuple.setDpid(tmpDpid);
            for (OFType type : OFType.values()) {
                resultTuple.getControlTrafficResults().put(type, Long.valueOf(elemSwitch.get(type.toString()).asString()));
                resultTuple.getControlTrafficByteResults().put(type, Long.valueOf(elemSwitch.get(type.toString()+"[bytes]").asString()));
            }
        }

        return results;
    }
}

class ControlPlaneMonitoringSanityException extends RuntimeException {
    public ControlPlaneMonitoringSanityException() {
        super();
    }

    public ControlPlaneMonitoringSanityException(String message) {
        super(message);
    }
}
