package mb.fw.atb.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathComparatorUtil {
	
	private final static Logger logger = LoggerFactory.getLogger(PathComparatorUtil.class);
	
	public static Comparator<Path> createLastModifiedTime(){
		Comparator<Path> comp = new Comparator<Path>() {
			@Override
			public int compare(Path o1, Path o2) {

				FileTime o1Time = null;
				FileTime o2Time = null;
				try {
					o1Time = Files.getLastModifiedTime(o1);
					o2Time = Files.getLastModifiedTime(o2);
				} catch (IOException e) {
					logger.warn("Sort Exception: ", e);
				}
				return o1Time.compareTo(o2Time);

			}
		};
		return comp;
	}
	
	

}
