package crash.commands.base2

import org.crsh.cli.Command
import org.crsh.cli.Usage

import java.text.SimpleDateFormat

class date {
    //java to groovy
    @Usage("show the current time")
    @Command
    def main(String format) {
        if (format == null) {
            format = "yyyy-MM-dd HH:mm:ss";
        }
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(date);
    }

}
