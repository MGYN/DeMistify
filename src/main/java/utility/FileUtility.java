package utility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class FileUtility {

	public static void wf(String path, String content, boolean append) {
		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(path, append)));
			out.println(content);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void wf_s(String path, List<String> content, boolean append) {
		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(path, append)));
			for (String file : content)
				out.println(file);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static ArrayList<String> readTxtFile(String filePath) {
		ArrayList<String> lineTxt = new ArrayList<String>();
		try {
			String encoding = "utf-8";
			File file = new File(filePath);
			if (file.isFile() && file.exists()) { // 判断文件是否存在
				InputStreamReader read = new InputStreamReader(new FileInputStream(file), encoding);// 考虑到编码格式
				BufferedReader bufferedReader = new BufferedReader(read);
				String line = null;
				while ((line = bufferedReader.readLine()) != null) {
					lineTxt.add(line);
				}
				read.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lineTxt;
	}
}
