import com.gigaspaces.metadata.SpaceTypeDescriptorBuilder;
import org.junit.Test;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.SpaceProxyConfigurer;

public class TypeDescriptorCreator {

    @Test
    public void Test() {
        GigaSpace space;
        SpaceProxyConfigurer configurer = new SpaceProxyConfigurer("demo");
        configurer = configurer.lookupLocators("192.168.X.X:4174");
        configurer = configurer.lookupGroups("xap-16.1.0");
        space = new GigaSpaceConfigurer(configurer).create();
        GigaSpace gigaSpace = space;

        // Your code goes here, for example:
        System.out.println("Entries in space: " + gigaSpace.count(null));

        registerSpaceTypeDescriptors(space);
    }

    private void registerSpaceTypeDescriptors(GigaSpace space) {
        SpaceTypeDescriptorBuilder employee = new SpaceTypeDescriptorBuilder("EMPLOYEE");
        employee.addFixedProperty("ID", String.class);
        employee.addFixedProperty("EMPNO", String.class);
        employee.addFixedProperty("FIRSTNME", String.class);
        employee.addFixedProperty("MIDINIT", String.class);
        employee.addFixedProperty("LASTNAME", String.class);
        employee.addFixedProperty("WORKDEPT", String.class);
        employee.addFixedProperty("PHONENO", String.class);
        employee.addFixedProperty("HIREDATE", String.class);
        employee.addFixedProperty("JOB", String.class);
        employee.addFixedProperty("EDLEVEL", String.class);
        employee.addFixedProperty("SEX", String.class);
        employee.addFixedProperty("BIRTHDATE", String.class);
//        employee.addFixedProperty("BIRTHDATE_CNV", Date.class);
        employee.addFixedProperty("SALARY", Double.class);
        employee.addFixedProperty("BONUS", Double.class);
        employee.addFixedProperty("COMM", String.class);
        employee.routingProperty("EMPNO");
        employee.idProperty("ID");
        space.getTypeManager().registerTypeDescriptor(employee.create());
    }

}
