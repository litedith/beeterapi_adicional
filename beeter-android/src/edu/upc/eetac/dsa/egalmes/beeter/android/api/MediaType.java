package edu.upc.eetac.dsa.egalmes.beeter.android.api;

public interface MediaType {
	public final static String BEETER_API_USER = "application/vnd.beeter.api.user+json";
	public final static String BEETER_API_USER_COLLECTION = "application/vnd.beeter.api.user.collection+json";
	public final static String BEETER_API_STING = "application/vnd.beeter.api.sting+json";//enviar o recibir stings
	public final static String BEETER_API_STING_COLLECTION = "application/vnd.beeter.api.sting.collection+json";//enviarcolecciones de sting
	public final static String BEETER_API_ERROR = "application/vnd.dsa.beeter.error+json";
}