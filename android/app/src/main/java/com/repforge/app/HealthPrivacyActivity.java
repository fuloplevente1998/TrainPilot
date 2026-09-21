package com.repforge.app;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.ScrollView;
public class HealthPrivacyActivity extends Activity {
 @Override public void onCreate(Bundle state){super.onCreate(state);TextView text=new TextView(this);text.setTextSize(18);text.setPadding(32,48,32,48);text.setText("RepForge – Health Connect adatkezelés\n\nA kapcsolat opcionális. A kiválasztott edzés időszakára olvasunk edzés-, pulzus- és aktívkalória-adatokat, az összesítés és az adatforrás megjelenítéséhez. A kalória a forrás becslése.\n\nAz importált adatok csak az alkalmazás memóriájában vannak, nem kerülnek a RepForge JSON-mentésébe, Google Drive-jára vagy külső kiszolgálóra. A nézetben elfelejthetők, az alkalmazásfolyamat megszűnésekor elvesznek.\n\nKülön edzésírási engedéllyel és külön gombnyomásra a RepForge az edzés nevét, típusát, kezdetét és végét írja a Health Connectbe. Nem másolja vissza az órás kalóriát vagy pulzust. Az írt adatot más engedélyezett alkalmazások is olvashatják.\n\nAz engedélyeket és az exportált bejegyzéseket a Health Connect rendszerbeállításaiban kezelheted és törölheted. A RepForge személyes profilja és saját edzésnaplója a meglévő mentési beállításokat követi.\n\nNincs hirdetési célú felhasználás vagy értékesítés.");ScrollView scroll=new ScrollView(this);scroll.addView(text);setContentView(scroll);}
}
