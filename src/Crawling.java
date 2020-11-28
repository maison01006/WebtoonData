import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Crawling {
	private static String dbUrl = "jdbc:mysql://localhost:3306/ziptoon?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC";
	private static String dbUser = "root";
	private static String dbPasswd = "0000";
	
	private static Connection conn;
	private static PreparedStatement ps;
	private static ResultSet rs;
	public Crawling() {
		try {
			
			Class.forName("com.mysql.cj.jdbc.Driver");					// JDBC����̹��� JVM�������� ��������
			conn = DriverManager.getConnection(dbUrl, dbUser, dbPasswd);	// DB �����ϱ�
		}
		catch (ClassNotFoundException cnfe) {
			System.out.println("JDBC ����̹� Ŭ������ ã�� �� �����ϴ� : " + cnfe.getMessage());
		}
		catch (Exception ex) {
			System.out.println("DB ���� ���� : " + ex.getMessage());
		}
	}

	public static void main(String[] args) {
		Crawling c = new Crawling();
		DaumCrawling cw = new DaumCrawling();
		String[] days ={"월","화","수","목","금","토","일"};
		String URL = "https://comic.naver.com/webtoon/weekday.nhn";
		try {
			Document doc = Jsoup.connect(URL).get();
			Elements elem = doc.select("div.col");
			String title="";
			String img="";
			String day="";
			
			for(int i=0;i<7;i++) {
				JSONArray ja = new JSONArray();
				for(Element e : elem.get(i).getElementsByAttribute("href")) {
					if(!e.select("img").isEmpty()) {
						title=e.select("img").attr("title");
						System.out.println(title);
						img=e.select("img").attr("src");
						day=days[i];
						
						
						
						c.webInfo(title,"https://comic.naver.com"+e.attr("href"),day);
						c.updateWebtoon(c.webList(title,"https://comic.naver.com"+e.attr("href")),title);
					}
				}	
			}

			for(int i=0;i<7;i++) {
				JSONArray dataJo = cw.getDay(i);
				for(int i2=0;i2<dataJo.size();i2++) {
					JSONObject webtoonJo = (JSONObject)dataJo.get(i2);
					JSONObject titleImgJo = (JSONObject)webtoonJo.get("pcThumbnailImage");
					cw.nickname = webtoonJo.get("nickname").toString(); 
					cw.title = webtoonJo.get("title").toString();
					cw.img = titleImgJo.get("url").toString();
					cw.introduction = webtoonJo.get("introduction").toString();
					cw.picture = webtoonJo.get("introduction").toString();
					cw.avg= Double.parseDouble(webtoonJo.get("averageScore").toString());
					
					c.insertWebtoonInfo(cw.title,cw.img,"daum",cw.avg,cw.introduction);
					c.insertWebtoonDay(cw.title, days[i]);
					
					JSONObject cartoonJo = (JSONObject)webtoonJo.get("cartoon");
					JSONArray tmpJa = (JSONArray)cartoonJo.get("genres");
					JSONObject tmpJo = (JSONObject)tmpJa.get(0);
					cw.category.add(tmpJo.get("name").toString());
					
					tmpJa = (JSONArray)cartoonJo.get("artists");
					tmpJo = (JSONObject)tmpJa.get(0);
					cw.picture = tmpJo.get("name").toString();
					tmpJo = (JSONObject)tmpJa.get(1);
					cw.writer = tmpJo.get("name").toString();
					
					
					
					if(cw.writer.equals(cw.picture)) {
						c.insertWriter(cw.picture.trim());
						c.insertWebtoonWriter(cw.title, c.getWriterId(cw.picture.trim()));
						
					}else {
						c.insertWriter(cw.writer.trim());
						c.insertWriter(cw.picture.trim());
						c.insertWebtoonWriter(cw.title, c.getWriterId(cw.writer.trim()));
						c.insertWebtoonWriter(cw.title, c.getWriterId(cw.picture.trim()));
					}
					
					tmpJa = (JSONArray)cartoonJo.get("categories");
					for(int i3=0;i3<tmpJa.size();i3++) {
						tmpJo = (JSONObject)tmpJa.get(i3);
						cw.category.add(tmpJo.get("name").toString());
					}
					for(int i3=0;i3<cw.category.size();i3++) {
						c.insertCategory(cw.category.get(i3));
						c.insertWebtoonCategory(cw.title, c.getCategoryId(cw.category.get(i3)));
					}
					
					cw.category.clear();
					
					JSONArray episodeJa = cw.getEpisode(cw.nickname.trim());
					
					for(int i3=0;i3<episodeJa.size();i3++) {
						JSONObject episodeJo = (JSONObject)episodeJa.get(i3);
						
						if(episodeJo.get("serviceType").equals("free")) {
					
							JSONObject imgJo = (JSONObject)episodeJo.get("thumbnailImage");
							
							c.insertWebtoonList(cw.title,episodeJo.get("title").toString() , "http://webtoon.daum.net/webtoon/viewer/"+episodeJo.get("id").toString(), imgJo.get("url").toString());
						}
					}
				}
			}			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void insertWebtoonInfo(String title,String img, String web,String intro) {
		
		String sql = "INSERT INTO webtoon_info (id,img,web,introduction) SELECT  ?, ? ,?,? FROM DUAL  WHERE 0= (SELECT count(*) FROM webtoon_info WHERE id = ?)";
		
		try(PreparedStatement ps= conn.prepareStatement(sql)){
		
			ps.setString(1, title);
			ps.setString(2, img);
			ps.setString(3, web);
			ps.setString(4,intro);
			ps.setString(5,title);
				
			ps.executeUpdate();

			}catch(SQLException e) {
				e.printStackTrace();
			}
	}
public void insertWebtoonInfo(String title,String img, String web,double avg,String intro) {
		
		String sql = "INSERT INTO webtoon_info (id,img,web,avg_rating,introduction) SELECT ?, ?, ? ,?,? FROM DUAL  WHERE 0= (SELECT count(*) FROM webtoon_info WHERE id = ?)";
		
		try(PreparedStatement ps= conn.prepareStatement(sql)){
		
			ps.setString(1, title);
			ps.setString(2, img);
			ps.setString(3, web);
			ps.setDouble(4, avg);
			ps.setString(5, intro);
			ps.setString(6,title);
				
			ps.executeUpdate();

			}catch(SQLException e) {
				e.printStackTrace();
			}
	}
	public void insertWebtoonDay(String title,String day) {
		String sql = "INSERT INTO webtoon_day (id,day) SELECT ?, ? FROM DUAL  WHERE 0= (SELECT count(*) FROM webtoon_day WHERE id = ? AND day = ?)";
		
		try(PreparedStatement ps= conn.prepareStatement(sql)){
		
			ps.setString(1, title);
			ps.setString(2, day);
			ps.setString(3, title);
			ps.setString(4, day);
			
			ps.executeUpdate();

			}catch(SQLException e) {
				e.printStackTrace();
			}
	}
	public void insertWebtoonList(String title,String episode,String url,String img) {
		
		String sql = "INSERT INTO webtoon (id,episode,url,img) SELECT ?, ?, ?, ? FROM DUAL  WHERE 0= (SELECT count(*) FROM webtoon WHERE id = ? AND episode = ?)";
		try(PreparedStatement ps= conn.prepareStatement(sql)){
		
			ps.setString(1,title);
			ps.setString(2, episode);
			ps.setString(3, url);
			ps.setString(4, img);
			ps.setString(5, title);
			ps.setString(6, episode);
			ps.executeUpdate();

			}catch(SQLException e) {
				System.out.println("1");
				e.printStackTrace();
			}
	}
	public double webList(String title,String link) {
		String img;
		String url;
		String episode;
		int num=0;
		double sum=0;
		try {
			Document doc = Jsoup.connect(link).get();
			Elements elem = doc.select("tbody");
			while(true) {
				for(Element e : elem.select("tr").not(".band_banner")) {
					img = e.select("img").attr("src");
					url = "https://comic.naver.com"+e.select("td.title").select("a").attr("href");
					episode = e.select("td.title").select("a").text();
					insertWebtoonList(title,episode,url,img);
					num++;
					sum+=Double.parseDouble(e.select("td").select("strong").text());
				}
				if(!doc.select(".page_wrap").select(".next").isEmpty()) {
					doc = Jsoup.connect("https://comic.naver.com"+doc.select(".next").attr("href")).get();
					elem = doc.select("tbody");
				}else 
					break;
			
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sum/num;
	}
	public void insertWriter(String name) {
		String sql = "INSERT INTO writer (id,name) SELECT null, ? FROM DUAL  WHERE 0= (SELECT count(*) FROM writer WHERE name = ?)";
		try(PreparedStatement ps= conn.prepareStatement(sql)){
			
			
			ps.setString(1, name);
			ps.setString(2, name);
			ps.executeUpdate();
				
			}catch(SQLException e) {
				System.out.println("insertWriter : "+name);
				e.printStackTrace();
			}
	}
	public void insertWebtoonWriter(String title, int writerId) {
		String sql = "INSERT INTO webtoon_writer (webtoon_id,writer_id) SELECT ?, ? FROM DUAL  WHERE 0= (SELECT count(*) FROM webtoon_writer WHERE webtoon_id = ? AND writer_id = ?)";
		try(PreparedStatement ps= conn.prepareStatement(sql)){
			
			ps.setString(1,title);
			ps.setInt(2, writerId);
			ps.setString(3,title);
			ps.setInt(4, writerId);
			ps.executeUpdate();

			}catch(SQLException e) {
				System.out.println("insertWebtoonWriter : "+title+", "+writerId);
				e.printStackTrace();
			}
	}
	public void insertWebtoonCategory(String title, int category) {
		String sql = "INSERT INTO webtoon_category (webtoon_id,category_id) SELECT ?, ? FROM DUAL  WHERE 0= (SELECT count(*) FROM webtoon_category WHERE webtoon_id = ? AND category_id = ?)";
		try(PreparedStatement ps= conn.prepareStatement(sql)){
			
			ps.setString(1,title);
			ps.setInt(2,category);
			ps.setString(3,title);
			ps.setInt(4,category);
			ps.executeUpdate();
				

			}catch(SQLException e) {
				System.out.println("insertWebtoonCategory : "+title+", "+category);
				e.printStackTrace();
			}
	}
	public void webInfo(String title,String url,String day) {
		try {
			Document doc = Jsoup.connect(url).get();
			Elements elem = doc.select(".comicinfo .detail");
			String[] writer =elem.select("h2 span").text().split("/");
			Elements elem2 = doc.select(".comicinfo .thumb");
			String img = elem2.select("img").attr("src");
			String introduction = elem.select("p").not(".detail_info").text();
			insertWebtoonInfo(title,img,"naver",introduction);
			insertWebtoonDay(title, day);
			
			for(int i=0;i<writer.length;i++) {
				insertWriter(writer[i].trim());
				insertWebtoonWriter(title,getWriterId(writer[i].trim()));
				
			}
			String[] category = elem.select("p .genre").text().split(",");
			for(int i=0;i<category.length;i++) {
				insertWebtoonCategory(title, getCategoryId(category[i].trim()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public Boolean getWebtoonInfo(String title) {
		String sql = "SELECT id FROM webtoon_info where id = ?";
		try(PreparedStatement ps= conn.prepareStatement(sql)){
			ps.setString(1, title);
			
			rs = ps.executeQuery();
			rs.next();
			if(rs.getString("id").isEmpty()) {
				rs.close();
				return false;
			}	
			rs.close();
		}catch (Exception e) {
			System.out.println("getWriterId : "+title);
			e.printStackTrace();
		}
		return true;
	}
	public int getWriterId(String name) throws SQLException {
		String sql = "SELECT id FROM writer where name = ?";
		int id=0;
		try(PreparedStatement ps= conn.prepareStatement(sql)){
			ps.setString(1, name);
			
			rs = ps.executeQuery();
			rs.next();
			id=rs.getInt("id");
			
			rs.close();

		}catch (Exception e) {
			System.out.println("getWriterId : "+name);
			e.printStackTrace();
		}
		return id;
	}
	public int getCategoryId(String category) throws SQLException {
		String sql = "SELECT id FROM category where category = ?";
		int id=0;
		try(PreparedStatement ps= conn.prepareStatement(sql)){
			ps.setString(1, category);
			
			rs = ps.executeQuery();
			rs.next();
			id=rs.getInt("id");
			
			rs.close();
		}catch (Exception e) {
			System.out.println("getCategoryId : "+category);
			e.printStackTrace();
		}
		return id; 
	}
	public void insertCategory(String category) {
		String sql = "INSERT INTO category (category) SELECT  ? FROM DUAL  WHERE 0= (SELECT count(*) FROM category WHERE category = ?)";
		try(PreparedStatement ps= conn.prepareStatement(sql)){
			
			ps.setString(1,category);
			ps.setString(2,category);

			ps.executeUpdate();
				

			}catch(SQLException e) {
				System.out.println("insertCategory : "+category);
				e.printStackTrace();
			}
	}
	public void updateWebtoon(double avg,String title) {
		String sql = "UPDATE webtoon_info set avg_rating = ? where id= ?";
		try(PreparedStatement ps= conn.prepareStatement(sql)){
			
			ps.setDouble(1,avg);
			ps.setString(2,title);

			ps.executeUpdate();
				

			}catch(SQLException e) {
				System.out.println("updateWebtoon : "+title);
				e.printStackTrace();
			}
	}
}
