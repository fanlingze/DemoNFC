package com.flz.demonfc;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.widget.EditText;
import android.os.Parcelable;
import android.widget.Toast;

public class BeamActivity extends AppCompatActivity implements
        CreateNdefMessageCallback, OnNdefPushCompleteCallback {

    private EditText mBeamText;

    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_beam);
        mBeamText = findViewById(R.id.edittext_beam_text);

        mNfcAdapter = mNfcAdapter.getDefaultAdapter(this);
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,getClass()), 0);

        mNfcAdapter.setNdefPushMessageCallback(this, this);
        mNfcAdapter.setOnNdefPushCompleteCallback(this, this);
    }

    @Override    /** 窗口处理完成 */
    public void onNdefPushComplete(NfcEvent event) {
        //Log.d("message", "complete");
    }

    @Override  /** 如果另外一台手机，靠近当前这部手机 */
    public NdefMessage createNdefMessage(NfcEvent event) {
        String text = mBeamText.getText().toString().trim();
        if ("".equals(text))
            text = "默认文本";
        /*
         *  "com.android.calculator2"  官方原生计算器包。
         *  当另外一部手机靠近这部手机时，会启动计算器。
         *
         * NdefMessage ndefMessage = new NdefMessage( new NdefRecord[] {
         * NdefRecord .createApplicationRecord("com.android.calculator2") });
         */
        NdefMessage ndefMessage = new NdefMessage( new NdefRecord[] {NdefRecord .createApplicationRecord("com.android.calculator2") });
        //NdefMessage ndefMessage = new NdefMessage(new NdefRecord[] { createTextRecord(text) });

        return ndefMessage;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mNfcAdapter != null)
            mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null,
                    null);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mNfcAdapter != null)
            mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    public void onNewIntent(Intent intent) {
        // 用于显示接收手机的信息。如果接收手机没有打开，会默认调用系统的黑界面。
        processIntent(intent);
    }

    /** 根据文本创建 NdefRecord 这个对象 */
    public NdefRecord createTextRecord(String text) {
        byte[] langBytes = Locale.CHINA.getLanguage().getBytes(
                Charset.forName("US-ASCII"));
        Charset utfEncoding = Charset.forName("UTF-8");
        byte[] textBytes = text.getBytes(utfEncoding);
        int utfBit = 0;
        char status = (char) (utfBit + langBytes.length);
        byte[] data = new byte[1 + langBytes.length + textBytes.length];
        data[0] = (byte) status;
        System.arraycopy(langBytes, 0, data, 1, langBytes.length);
        System.arraycopy(textBytes, 0, data, 1 + langBytes.length,
                textBytes.length);
        NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.RTD_TEXT, new byte[0], data);

        return record;
    }

    void processIntent(Intent intent) {
        Parcelable[] rawMsgs = intent
                .getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

        NdefMessage msg = (NdefMessage) rawMsgs[0];
        String text = parseTextRecord(msg.getRecords()[0]);
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    /**
     * 解析NDEF文本数据，从第三个字节开始，后面的文本数据
     *
     * @param ndefRecord
     * @return
     */
    public static String parseTextRecord(NdefRecord ndefRecord) {
        /**
         * 判断数据是否为NDEF格式
         */
        //判断TNF
        if (ndefRecord.getTnf() != NdefRecord.TNF_WELL_KNOWN) {
            return null;
        }
        //判断可变的长度的类型
        if (!Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
            return null;
        }
        try {
            //获得字节数组，然后进行分析
            byte[] payload = ndefRecord.getPayload();
            //下面开始NDEF文本数据第一个字节，状态字节
            //判断文本是基于UTF-8还是UTF-16的，取第一个字节"位与"上16进制的80，16进制的80也就是最高位是1，
            //其他位都是0，所以进行"位与"运算后就会保留最高位
            String textEncoding = ((payload[0] & 0x80) == 0) ? "UTF-8" : "UTF-16";
            //3f最高两位是0，第六位是1，所以进行"位与"运算后获得第六位
            int languageCodeLength = payload[0] & 0x3f;
            //下面开始NDEF文本数据第二个字节，语言编码
            //获得语言编码
            String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            //下面开始NDEF文本数据后面的字节，解析出文本
            String textRecord = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
            return textRecord;
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }
    }
}
