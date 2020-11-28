import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.stream.MemoryCacheImageInputStream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class DaumCrawling {

	String nickname = null;
	String title = null;
	String webtoonUrl = null;
	String img = null;
	String writer = null;
	String picture = null;
	String introduction = null;
	Double avg;
	List<String> category = new ArrayList<String>();
	
	public JSONArray getDay(int index) {
		JSONObject jo = new JSONObject();
		JSONArray ja = new JSONArray();
		JSONParser jp = new JSONParser();
		
		
		String[] days = {"mon","tue","wed","thu","fri","sat","sun"};
		String line ="";
		
		try {
			URL	url = new URL("http://webtoon.daum.net/data/pc/webtoon/list_serialized/"+days[index]);
		
			HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
			urlConnection.setRequestMethod("GET");
			BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));
			StringBuilder sb = new StringBuilder();

			while((line = br.readLine())!=null) {
				sb.append(line);
			}

			jo = (JSONObject)jp.parse(sb.toString());
			
			ja = (JSONArray)jo.get("data");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return ja;
	}
	public JSONArray getEpisode (String nickname) {
		JSONObject jo = new JSONObject();
		JSONArray episodeJa = new JSONArray();
		JSONParser jp = new JSONParser();
		JSONObject dataJo = new JSONObject();
		String line ="";
		try {
			URL	url = new URL("http://webtoon.daum.net/data/pc/webtoon/view/"+nickname);
		
			HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
			urlConnection.setRequestMethod("GET");
			BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));
			StringBuilder sb = new StringBuilder();

			while((line = br.readLine())!=null) {
				sb.append(line);
			}

			jo = (JSONObject)jp.parse(sb.toString());
			
			dataJo = (JSONObject)jo.get("data");
			
			JSONObject webtoonJo = (JSONObject)dataJo.get("webtoon");
			episodeJa = (JSONArray)webtoonJo.get("webtoonEpisodes");
			
		} catch (Exception e) {
			System.out.println("getEpisode : "+nickname);
			e.printStackTrace();
		}
		
		return episodeJa;
	}

}
