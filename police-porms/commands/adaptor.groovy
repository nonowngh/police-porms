package crash.commands.base2

import com.mb.indigo2.springsupport.AdaptorConfigBean
import org.crsh.cli.Command
import org.crsh.cli.Man
import org.crsh.cli.Usage

import java.text.SimpleDateFormat
@Usage("Adaptor commands")
@Man("""\
Usage......""")
class adaptor {
    //java to groovy
    @Usage("adaptor info")
    @Command
    def info() {
        AdaptorConfigBean bean = context.attributes.beans["adaptorBean"];
        def port = bean.getPort();
        def name = bean.getAdaptorName();
        def properties = bean.getProperties();

        context.provide([name:"adaptor",value:bean?.adaptorName]);
        context.provide([name:"port",value:bean?.port]);
        context.provide([name:"property",value:properties.toString()]);

    }

}
