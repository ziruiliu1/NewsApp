package com.newsapp.android.TabAdapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;


import com.google.gson.Gson;
import com.newsapp.android.MainActivity;
import com.newsapp.android.R;
import com.newsapp.android.UserMode.DBOpenHelper;
import com.newsapp.android.WebActivity;
import com.newsapp.android.gson.NewsBean;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class NewsFragment extends Fragment {
    private List<NewsBean.ResultBean.DataBean> list;
    private ListView listView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton fab;
    String usernumbbbb;
    private static final int UPNEWS_INSERT = 0;
    private static int zhizhen =0;
    @SuppressLint("HandlerLeak")
    private Handler newsHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            String uniquekey,title,date, category,author_name,url,thumbnail_pic_s,thumbnail_pic_s02,thumbnail_pic_s03;
            switch (msg.what){
                case UPNEWS_INSERT:
                    list = ((NewsBean) msg.obj).getResult().getData();
                    MyTabAdapter adapter = new MyTabAdapter(getActivity(),list);
                    listView.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                    break;
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list_item,container,false);
        listView = (ListView) view.findViewById(R.id.listView);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh);
        fab = (FloatingActionButton) view.findViewById(R.id.fab);
        view.findViewById(R.id.fab).bringToFront();
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //获取宿本Activity的number值
        onAttach(getActivity());
        Bundle bundle = getArguments();
        final String data = bundle.getString("name","top");
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*计划进行置顶操作*/
                listView.smoothScrollToPosition(0);
            }
        });
        //下拉刷新
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
               new Handler().postDelayed(new Runnable() {
                   @Override
                   public void run() {
                       swipeRefreshLayout.setRefreshing(false);
                   }
               },3000);
            }
        });

        //异步加载数据
        getDataFromNet(data);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //获取点击条目的路径，传值显示webview页面
                String url = list.get(position).getUrl();
                String uniquekey = list.get(position).getUniquekey();
                final   NewsBean.ResultBean.DataBean dataBean = (NewsBean.ResultBean.DataBean) list.get(position);

                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        Connection conn = null;
                        conn = (Connection) DBOpenHelper.getConn();
                        System.out.print("******************");
                        System.out.print(conn);
                        System.out.print("********************");
                        String sql = "insert into news_info(uniquekey,title,date,category,author_name,url,thumbnail_pic_s,thumbnail_pic_s02,thumbnail_pic_s03) values(?,?,?,?,?,?,?,?,?)";
                        int i = 0;

                        PreparedStatement pstmt;
                        try {
                            pstmt = (PreparedStatement) conn.prepareStatement(sql);
                            pstmt.setString(1,dataBean.getUniquekey());
                            pstmt.setString(2,dataBean.getTitle());
                            pstmt.setString(3,dataBean.getDate());
                            pstmt.setString(4,dataBean.getCategory());
                            pstmt.setString(5,dataBean.getAuthor_name());
                            pstmt.setString(6,dataBean.getUrl());
                            pstmt.setString(7,dataBean.getThumbnail_pic_s());
                            pstmt.setString(8,dataBean.getThumbnail_pic_s02());
                            pstmt.setString(9,dataBean.getThumbnail_pic_s03());
                            i = pstmt.executeUpdate();

                            pstmt.close();
                            conn.close();

                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                Intent intent = new Intent(getActivity(),WebActivity.class);
                intent.putExtra("url",url);
                intent.putExtra("uniquekey",uniquekey);
                intent.putExtra("usernumbbbb",usernumbbbb);
                startActivity(intent);
            }
        });
    }
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        usernumbbbb = ((MainActivity) activity).getPhonenumber();//通过强转成宿主activity，就可以获取到传递过来的数据
        System.out.println("fanhuishide1shuju++**&^%%$$$##"+usernumbbbb);
    }

    private void getDataFromNet(final String data){
       @SuppressLint("StaticFieldLeak") AsyncTask<Void,Void,String> task = new AsyncTask<Void, Void, String>() {
           @Override
           protected String doInBackground(Void... params) {
               String path = "http://v.juhe.cn/toutiao/index?type="+data+"&key=547ee75ef186fc55a8f015e38dcfdb9a";
               URL url = null;
               if (zhizhen == 90&& zhizhen <= 180){
                   path="http://v.juhe.cn/toutiao/index?type="+data+"&key=9eacc1a90ba2f55c116bfd7a16e26bc3";
               }
               try {
                   url = new URL(path);
                   HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                   connection.setRequestMethod("GET");
                   connection.setReadTimeout(5000);
                   connection.setConnectTimeout(5000);

                   int responseCode = connection.getResponseCode();
                   if (responseCode == 200){
                       zhizhen++;
                       InputStream inputStream = connection.getInputStream();
                       String json = streamToString(inputStream,"utf-8");
                       return json;
                   } else {
                       System.out.println(responseCode);
                       return "已达到今日访问次数上限";
                   }

               } catch (MalformedURLException e) {
                   e.printStackTrace();
               } catch (ProtocolException e) {
                   e.printStackTrace();
               } catch (IOException e) {
                   e.printStackTrace();
               }
               return "";
           }
           protected void onPostExecute(final String result){
               new Thread(new Runnable() {
                   @Override
                   public void run() {
                       NewsBean newsBean = new Gson().fromJson(result,NewsBean.class);
                       System.out.println("6666666666");
                       System.out.println(newsBean.getError_code());
                       if ("10012".equals(""+newsBean.getError_code())){
                           System.out.println("77777777");
                           List<NewsBean.ResultBean.DataBean> listDataBean = new ArrayList<>();
                           Connection conn = null;
                           conn = (Connection) DBOpenHelper.getConn();
                           String sql = "select * from news_info ";
                           PreparedStatement pstmt;
                           try {
                               pstmt = (PreparedStatement) conn.prepareStatement(sql);
                               ResultSet rs = pstmt.executeQuery();
                               while (rs.next()){
                                   NewsBean.ResultBean.DataBean dataBean = new NewsBean.ResultBean.DataBean();
                                   dataBean.setUniquekey(rs.getString(1));
                                   dataBean.setTitle(rs.getString(2));
                                   dataBean.setDate(rs.getString(3));
                                   dataBean.setCategory(rs.getString(4));
                                   dataBean.setAuthor_name(rs.getString(5));
                                   dataBean.setUrl(rs.getString(6));
                                   dataBean.setThumbnail_pic_s(rs.getString(7));
                                   dataBean.setThumbnail_pic_s02(rs.getString(8));
                                   dataBean.setThumbnail_pic_s03(rs.getString(9));
                                   listDataBean.add(dataBean);


                               }
                               newsBean.setResult(new NewsBean.ResultBean());
                               newsBean.getResult().setData(listDataBean);
                               pstmt.close();
                               conn.close();
                               System.out.println(newsBean.getResult().getData());
                           } catch (SQLException e) {
                               e.printStackTrace();
                           }
                       }
                       Message msg=newsHandler.obtainMessage();
                       msg.what=UPNEWS_INSERT;
                       msg.obj = newsBean;
                       newsHandler.sendMessage(msg);
                   }
               }).start();
           }
       };
          task.execute();
    }

    private String streamToString(InputStream inputStream, String charset){
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream,charset);

            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String s = null;
            StringBuilder builder = new StringBuilder();
            while ((s = bufferedReader.readLine()) != null){
                builder.append(s);
            }
            bufferedReader.close();
            return builder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }
}
