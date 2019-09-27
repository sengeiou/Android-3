package com.liz.whatsai.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.liz.androidutils.FileUtils;
import com.liz.whatsai.R;
import com.liz.whatsai.app.AudioListAdapter;
import com.liz.whatsai.logic.ComDef;
import com.liz.whatsai.logic.WhatsaiAudio;

import java.io.File;

public class AudioActivity extends Activity implements View.OnClickListener,
        View.OnCreateContextMenuListener {

    private ImageButton mBtnSwitchAudio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);

        findViewById(R.id.titlebar_menu).setOnClickListener(this);
        findViewById(R.id.titlebar_close).setOnClickListener(this);

        mBtnSwitchAudio = findViewById(R.id.btn_switch_audio);
        mBtnSwitchAudio.setOnClickListener(this);

        ListView listView = findViewById(R.id.lv_audio_files);
        listView.addFooterView(new ViewStub(this));
        listView.setAdapter(AudioListAdapter.getAdapter());
        //listView.setOnItemClickListener(this);
        listView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                for (ComDef.AudioListMenu c : ComDef.AudioListMenu.values()) {
                    menu.add(0, c.id, 0, c.name);
                }
            }
        });

        setAudioFilesInfo();
        WhatsaiAudio.setAudioCallback(new WhatsaiAudio.WhatsaiAudioCallback() {
            @Override
            public void onAudioFileGenerated() {
                AudioActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateAudioList();
                    }
                });
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.titlebar_menu:
            case R.id.titlebar_close:
                onBackPressed();
                break;
            case R.id.btn_switch_audio:
                onSwitchAudio();
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int itemId = item.getItemId();
        if (itemId == ComDef.AudioListMenu.PLAY.id) {
            File f = AudioListAdapter.getAudioFile(info.id);
            Toast.makeText(this, "open " + f.getAbsolutePath(), Toast.LENGTH_LONG).show();
            return true;
        }
        else if (itemId == ComDef.AudioListMenu.STOP.id) {
            //####@:onOpenNode(info.id);
            return true;
        }
        else if (itemId == ComDef.AudioListMenu.DEL.id) {
            onDeleteAudioFile(info.id);
            return true;
        }
        else {
            return super.onContextItemSelected(item);
        }
    }

    private void onDeleteAudioFile(long id) {
        final int pos = (int)id;
        final File f = AudioListAdapter.getAudioFile(pos);
        final TextView tv = new TextView(this);
        tv.setText(f.getAbsolutePath());
        new AlertDialog
                .Builder(this)
                .setTitle("Are You Sure to Delete Audio File: ")
                .setIcon(android.R.drawable.sym_def_app_icon)
                .setView(tv)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        FileUtils.removeFile(f);
                        updateAudioList();
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    private void updateAudioList() {
        AudioListAdapter.onUpdateList();
        setAudioFilesInfo();
    }

    private void setAudioFilesInfo() {
        TextView textAudioFiles = findViewById(R.id.tv_audio_files);
        textAudioFiles.setText(AudioListAdapter.getAudioFilesInfo());
    }

    protected void onSwitchAudio() {
        WhatsaiAudio.switchAudio();
        mBtnSwitchAudio.setBackgroundResource(WhatsaiAudio.isRecording() ? R.drawable.bg_circle_green : R.drawable.bg_circle_red);
    }
}