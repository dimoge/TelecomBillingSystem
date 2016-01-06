package me.tbs.zhang.activity;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import me.tbs.zhang.R;
import me.tbs.zhang.entity.Record;
import me.tbs.zhang.utils.DBHelper;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity {

    DBHelper dbHelper;
    SQLiteDatabase sqLiteDatabase;

    Button findBtn, main_import;
    EditText input_edit;
    ListView listView;
    TextView uname, longExpense, shortExpense;

    List<Record> dataList = new ArrayList<Record>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(MainActivity.this);
        sqLiteDatabase = dbHelper.getReadableDatabase();
        //初始化控件
        findBtn = (Button) findViewById(R.id.main_btn);
        main_import = (Button) findViewById(R.id.main_import);
        input_edit = (EditText) findViewById(R.id.main_edit_text);
        listView = (ListView) findViewById(R.id.main_listview);
        uname = (TextView) findViewById(R.id.main_uname);
        longExpense = (TextView) findViewById(R.id.main_long_mongy_tv);
        shortExpense = (TextView) findViewById(R.id.main_short_money_tv);

        findBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dataList.clear();
                String targetTel = input_edit.getText().toString().trim();
                if(targetTel.equals("")  || targetTel == null){
                    Toast.makeText(MainActivity.this, "请输入号码", Toast.LENGTH_LONG).show();
                }else{
                    Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM record WHERE tel = '"+targetTel+"'", null);
                    if(cursor.getCount()>0){
                        float durationLong = 0;//长途电话分钟数
                        float durationShort = 0;//短途电话分钟数
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        while (cursor.moveToNext()){
                            Record record = new Record();
                            record.setId(cursor.getPosition());
                            record.setTel(cursor.getString(cursor.getColumnIndex("tel")));
                            record.setName(cursor.getString(cursor.getColumnIndex("name")));
                            try {
                                record.setDate( sdf.parse(cursor.getString(cursor.getColumnIndex("date"))) );
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            record.setDuration(cursor.getFloat(cursor.getColumnIndex("duration")));
                            record.setType(cursor.getString(cursor.getColumnIndex("type")).equals("true")?true:false);
                            if(record.isType()){
                                durationLong  = + record.getDuration();
                            }else{
                                durationShort = + record.getDuration();
                            }
                            dataList.add(record);
                            listView.setAdapter(new MyAdapter());
                        }
                        //显示统计
                        longExpense.setText(durationLong*1.5 + "元");
                        shortExpense.setText(durationShort*0.3 + "元");
                        uname.setText(dataList.get(0).getName());
                    }else{
                        Toast.makeText(MainActivity.this, "未搜索到数据 :(", Toast.LENGTH_LONG).show();
                    }

                }
                listView.deferNotifyDataSetChanged();
            }
        });

        main_import.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, FilesActivity.class));
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        String path = getIntent().getStringExtra("file_path");
        if(path == null || path.equals("")){
            return;
        }
        File txtFile = new File(path);//获得选择的txt文件
        getStringFromFile(txtFile);

    }

    public void getStringFromFile(File file) {
        try {
            StringBuffer sBuffer = new StringBuffer();
            FileInputStream fInputStream = new FileInputStream(file);
            InputStreamReader inputStreamReader = new InputStreamReader(fInputStream, "utf-8");
            BufferedReader in = new BufferedReader(inputStreamReader);
            while (in.ready()) {
                sBuffer.append(in.readLine() + "\n");
                JSONObject jsonObject = new JSONObject(in.readLine());
                ContentValues contentValues = new ContentValues();
                contentValues.put("tel", jsonObject.get("tel").toString());
                contentValues.put("name", jsonObject.get("name").toString());
                contentValues.put("date", jsonObject.get("date").toString());
                contentValues.put("duration", jsonObject.get("duration").toString());
                contentValues.put("type", jsonObject.get("type").toString());
                long resu = sqLiteDatabase.insert("record", null, contentValues);//存入数据库中
                resu++;
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class MyAdapter extends BaseAdapter {

        LayoutInflater layoutInflater;
        TextView number_tv, type_tv, duration_tv, expense_tv;

        @Override
        public int getCount() {
            return dataList.size();
        }

        @Override
        public Object getItem(int i) {
            return dataList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (layoutInflater == null) {
                layoutInflater = LayoutInflater.from(MainActivity.this);
            }
            view = layoutInflater.inflate(R.layout.main_item, null);
            number_tv = (TextView) view.findViewById(R.id.number_tv);
            type_tv = (TextView) view.findViewById(R.id.type_tv);
            duration_tv = (TextView) view.findViewById(R.id.duration_tv);
            expense_tv = (TextView) view.findViewById(R.id.expense_tv);

            number_tv.setText(dataList.get(i).getTel());
            type_tv.setText(dataList.get(i).isType()?"长途电话":"本地电话");
            expense_tv.setText(dataList.get(i).getDuration()+"分钟");

            return view;
        }
    }

}
