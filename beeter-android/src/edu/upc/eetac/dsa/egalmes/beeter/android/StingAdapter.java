package edu.upc.eetac.dsa.egalmes.beeter.android;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import edu.upc.eetac.dsa.egalmes.beeter.android.api.Sting;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class StingAdapter extends BaseAdapter {
private ArrayList <Sting> data;
private LayoutInflater inflater;	

private static class ViewHolder {
	TextView tvSubject;
	TextView tvUsername;
	TextView tvDate;
}
	public StingAdapter(Context context, ArrayList<Sting> data) {
		super();
		inflater = LayoutInflater.from(context);
		this.data = data;
	}
	
	@Override
	public int getCount() {//devuelve numero total de filas que habria en la lista , numero de datos q tu muestras
		// TODO Auto-generated method stub
		return data.size();
	}

	@Override
	public Object getItem(int position) {//modelo, cada posicion de la lista tiene un modelo de la cual tiene una serie de datos
		// TODO Auto-generated method stub
		return data.get(position);
	}

	@Override
	public long getItemId(int position) {//devuelve valor unico para una determinada posicion
		// TODO Auto-generated method stub
		return Long.parseLong(((Sting)getItem(position)).getId());
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {//devueve ese layout qe hemos creado cn datos
		// TODO Auto-generated method stub
		ViewHolder viewHolder = null;
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.list_row_sting, null);//listrowsting que habiamos creado
			viewHolder = new ViewHolder();
			viewHolder.tvSubject = (TextView) convertView
					.findViewById(R.id.tvSubject);
			viewHolder.tvUsername = (TextView) convertView
					.findViewById(R.id.tvUsername);
			viewHolder.tvDate = (TextView) convertView
					.findViewById(R.id.tvDate);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		String subject = data.get(position).getSubject();//recuperando valores de esa posicion
		String username = data.get(position).getUsername();
		String date = SimpleDateFormat.getInstance().format(
				data.get(position).getLastModified());
		viewHolder.tvSubject.setText(subject);
		viewHolder.tvUsername.setText(username);
		viewHolder.tvDate.setText(date);
		return convertView;
	}

}
