package utils;

import java.io.File;
import java.io.FileOutputStream;

import de.erichseifert.gral.data.DataTable;
import de.erichseifert.gral.io.data.DataWriter;
import de.erichseifert.gral.io.data.DataWriterFactory;

public class DataTableUtils {
	public static void storeTable(String filename, DataTable table) {
		File file = new File(filename);
		DataWriter writer = DataWriterFactory.getInstance().get("text/csv");
		try {
			writer.write(table, new FileOutputStream(file));
		} catch (Exception e) {
			System.err.println("DataTableUtils: Problem storing data: " + file);
			System.exit(-1);
		}
	}
}
