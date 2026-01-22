package mb.fw.atb.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The type Date utils.
 */
@Slf4j
public class DateUtils {

    /**
     * The Simple date format.
     */
    static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    /**
     * The Formater.
     */
    static DateTimeFormatter formater = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Today 8 string.
     *
     * @return the string
     */
    public static String today8() {
        return DateFormatUtils.format(new Date(), "yyyyMMdd");
    }

    /**
     * Todaymmdd string.
     *
     * @return the string
     */
    public static String todaymmdd() {
        return DateFormatUtils.format(new Date(), "MMdd");
    }

    /**
     * To daydd string.
     *
     * @return the string
     */
    public static String toDaydd() {
        return DateFormatUtils.format(new Date(), "dd");
    }

    /**
     * Today 14 string.
     *
     * @return the string
     */
    public static String today14() {
        return DateFormatUtils.format(new Date(), "yyyyMMddHHmmss");
    }

    /**
     * Today 17 string.
     *
     * @return the string
     */
    public static String today17() {
        return DateFormatUtils.format(new Date(), "yyyyMMddHHmmssSSS");
    }

    /**
     * Today 20 string.
     *
     * @return the string
     */
    public static String today20() {
        return DateFormatUtils.format(new Date(), "yyyyMMddHHmmssSSSSSS");
    }

    /**
     * Today m mdd h hmmss string.
     *
     * @return the string
     */
    public static String todayMMddHHmmss() {
        return DateFormatUtils.format(new Date(), "MMddHHmmss");
    }
    public static String todayyyyy() {
        return DateFormatUtils.format(new Date(), "yyyy");
    }

    /**
     * Gets dates between to list.
     *
     * @param startDtStr the start dt str
     * @param endDtStr   the end dt str
     * @return the dates between to list
     * @throws ParseException the parse exception
     */
    public static List<String> getDatesBetweenToList(final String startDtStr, final String endDtStr) throws ParseException {
        LocalDate start = LocalDate.parse(startDtStr , formater);
        LocalDate end = LocalDate.parse(endDtStr , formater);
        List<String> dates = new ArrayList<>();
        dates.add(start.format(formater));
        while (start.isBefore(end)) {
            start = start.plusDays(1);
            dates.add(start.format(formater));
        }
        return dates;
    }


    /**
     * The entry point of application.
     *
     * @param args the input arguments
     * @throws ParseException the parse exception
     */
    public static void main(String[] args) throws ParseException {
        log.info(getDatesBetweenToList("20220601", "20221201").toString());
    }

}
