package edu.upc.eetac.dsa.egalmes.beeter.android.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
 


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
 


import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
 
public class BeeterAPI {
	private final static String TAG = BeeterAPI.class.getName();
	private static BeeterAPI instance = null;
	private URL url;
 
	private BeeterRootAPI rootAPI = null;
 
	private BeeterAPI(Context context) throws IOException,
			BeeterAndroidException {
		super();
 
		AssetManager assetManager = context.getAssets();
		Properties config = new Properties();
		config.load(assetManager.open("config.properties"));//carga fichero configuracion 
		String serverAddress = config.getProperty("server.address");//obtiene los valores de es fichero
		String serverPort = config.getProperty("server.port");
		url = new URL("http://" + serverAddress + ":" + serverPort
				+ "/beeter-api"); //se qeda cn la base url esta si utilizamos hateoas nunca cambia
 
		Log.d("LINKS", url.toString());
		getRootAPI();
	}
 
	public final static BeeterAPI getInstance(Context context)
			throws BeeterAndroidException {
		if (instance == null)
			try {
				instance = new BeeterAPI(context);//context es la actividad, para recuperar valores del fichero conf.
			} catch (IOException e) {
				throw new BeeterAndroidException(
						"Can't load configuration file");
			}
		return instance;
	}
 
	private void getRootAPI() throws BeeterAndroidException { //rea un modelo y ataka al servicio
		Log.d(TAG, "getRootAPI()");
		rootAPI = new BeeterRootAPI();
		HttpURLConnection urlConnection = null;
		try {
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("GET");
			urlConnection.setDoInput(true);// true por defecto, significa que qiero leer
			urlConnection.connect();
		} catch (IOException e) {
			throw new BeeterAndroidException(
					"Can't connect to Beeter API Web Service");
		}
 
		BufferedReader reader;
		try {//lee json que le devuelve htps://localhost:8080/beeterapi
			reader = new BufferedReader(new InputStreamReader(
					urlConnection.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
 
			JSONObject jsonObject = new JSONObject(sb.toString());// aparti de un string y objeto json lo convierte
			JSONArray jsonLinks = jsonObject.getJSONArray("links");//asi poder manipular y obtener get, arrays.. 
			parseLinks(jsonLinks, rootAPI.getLinks());//lo proceso con el metodo priado de esta clase y lo guardas en el modelo rootAPI
		} catch (IOException e) {
			throw new BeeterAndroidException(
					"Can't get response from Beeter API Web Service");
		} catch (JSONException e) {
			throw new BeeterAndroidException("Error parsing Beeter Root API");
		}
 
	}
 
	public StingCollection getStings() throws BeeterAndroidException {
		Log.d(TAG, "getStings()");
		StingCollection stings = new StingCollection();
 
		HttpURLConnection urlConnection = null;
		try {
			urlConnection = (HttpURLConnection) new URL(rootAPI.getLinks()
					.get("stings").getTarget()).openConnection();
			urlConnection.setRequestMethod("GET");
			urlConnection.setDoInput(true);
			urlConnection.connect();
		} catch (IOException e) {
			throw new BeeterAndroidException(
					"Can't connect to Beeter API Web Service");
		}
 
		BufferedReader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(
					urlConnection.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
 
			JSONObject jsonObject = new JSONObject(sb.toString());
			JSONArray jsonLinks = jsonObject.getJSONArray("links");//atributoss
			parseLinks(jsonLinks, stings.getLinks());
 
			stings.setNewestTimestamp(jsonObject.getLong("newestTimestamp"));//atributo
			stings.setOldestTimestamp(jsonObject.getLong("oldestTimestamp"));//atributo qe recuperamos
			JSONArray jsonStings = jsonObject.getJSONArray("stings");
			for (int i = 0; i < jsonStings.length(); i++) {
				Sting sting = new Sting();//creo un sting
				JSONObject jsonSting = jsonStings.getJSONObject(i);// le doy valor a traves del array y lo añado a la coleccion qe es lo qe lo devuelves
				sting.setAuthor(jsonSting.getString("author"));
				sting.setId(jsonSting.getString("id"));//se van añadiendo
				sting.setLastModified(jsonSting.getLong("lastModified"));
				sting.setSubject(jsonSting.getString("subject"));
				sting.setUsername(jsonSting.getString("username"));
				jsonLinks = jsonSting.getJSONArray("links");
				parseLinks(jsonLinks, sting.getLinks());
				stings.getStings().add(sting);
			}
		} catch (IOException e) {
			throw new BeeterAndroidException(
					"Can't get response from Beeter API Web Service");
		} catch (JSONException e) {
			throw new BeeterAndroidException("Error parsing Beeter Root API");
		}
 
		return stings;
	}
 
	private void parseLinks(JSONArray jsonLinks, Map<String, Link> map)
			throws BeeterAndroidException, JSONException {
		for (int i = 0; i < jsonLinks.length(); i++) {
			Link link = SimpleLinkHeaderParser
					.parseLink(jsonLinks.getString(i));
			//REL PODIA ser multiple rel=" home boomark self" -> 3 enlaces qe obtienes a traves del mapa
			String rel = link.getParameters().get("rel");//tb podria obteet el title i el target(?) pRA QITARME LOS ESPACIOS BLANCOS DE ENCIAM
			String rels[] = rel.split("\\s");
			for (String s : rels)
				map.put(s, link);
		}
	}
	
	public Sting getSting(String urlSting) throws BeeterAndroidException {
		Sting sting = new Sting();
	 
		HttpURLConnection urlConnection = null;
		try {
			URL url = new URL(urlSting);
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("GET");
			urlConnection.setDoInput(true);
			urlConnection.connect();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					urlConnection.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
			JSONObject jsonSting = new JSONObject(sb.toString()); //revuperado json, i procesado json
			sting.setAuthor(jsonSting.getString("author"));
			sting.setId(jsonSting.getString("id"));
			sting.setLastModified(jsonSting.getLong("lastModified"));
			sting.setSubject(jsonSting.getString("subject"));
			sting.setContent(jsonSting.getString("content"));
			sting.setUsername(jsonSting.getString("username"));
			JSONArray jsonLinks = jsonSting.getJSONArray("links");
			parseLinks(jsonLinks, sting.getLinks());
		} catch (MalformedURLException e) {
			Log.e(TAG, e.getMessage(), e);
			throw new BeeterAndroidException("Bad sting url");
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
			throw new BeeterAndroidException("Exception when getting the sting");
		} catch (JSONException e) {
			Log.e(TAG, e.getMessage(), e);
			throw new BeeterAndroidException("Exception parsing response");
		}
	 
		return sting;
	}
	public Sting createSting(String subject, String content) throws BeeterAndroidException {
		Sting sting = new Sting();
		sting.setSubject(subject);
		sting.setContent(content);
		HttpURLConnection urlConnection = null;
		try {
			JSONObject jsonSting = createJsonSting(sting);
			URL urlPostStings = new URL(rootAPI.getLinks().get("create-stings")
					.getTarget());
			urlConnection = (HttpURLConnection) urlPostStings.openConnection();
			urlConnection.setRequestProperty("Accept",
					MediaType.BEETER_API_STING);
			urlConnection.setRequestProperty("Content-Type",
					MediaType.BEETER_API_STING);
			urlConnection.setRequestMethod("POST");
			urlConnection.setDoInput(true);
			urlConnection.setDoOutput(true);
			urlConnection.connect();
			PrintWriter writer = new PrintWriter(
					urlConnection.getOutputStream());
			writer.println(jsonSting.toString());
			writer.close();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					urlConnection.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
			jsonSting = new JSONObject(sb.toString());
	 
			sting.setAuthor(jsonSting.getString("author"));
			sting.setId(jsonSting.getString("id"));
			sting.setLastModified(jsonSting.getLong("lastModified"));
			sting.setSubject(jsonSting.getString("subject"));
			sting.setContent(jsonSting.getString("content"));
			sting.setUsername(jsonSting.getString("username"));
			JSONArray jsonLinks = jsonSting.getJSONArray("links");
			parseLinks(jsonLinks, sting.getLinks());
		} catch (JSONException e) {
			Log.e(TAG, e.getMessage(), e);
			throw new BeeterAndroidException("Error parsing response");
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
			throw new BeeterAndroidException("Error getting response");
		} finally {
			if (urlConnection != null)
				urlConnection.disconnect();
		}
		return sting;
	}
	//writesting activity progrso void i sting tipo de retorno stings..params (aqi esta tanto el subject como el content) 
	//onpostexecute recarga lista con todos los stings inclusive el nuevo k hemos creado
	//oncreate carga layout
	//dosmetodos poststing y cancel
	//finish acaba actividad y vuelve a la anterior en este caso a la lista de stings , mostrat actividad tal i como estaba
	//en el showstings parecido al finish pero con la lista actualizada
	//post obtenemos  do input leemos, doutpu envamios, createstingjson, atraves de el metodo pivado, se crea json object i se van colocando valores 
	private JSONObject createJsonSting(Sting sting) throws JSONException {
		JSONObject jsonSting = new JSONObject();
		jsonSting.put("subject", sting.getSubject());
		jsonSting.put("content", sting.getContent());
	 
		return jsonSting;
	}
}