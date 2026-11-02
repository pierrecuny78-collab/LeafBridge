package org.leafvibe.leafbridge

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import android.text.Editable
import android.text.TextWatcher

/** Local catalogue and registry. No third-party code is downloaded or executed here. */
class ExtensionLibraryActivity : Activity() {
    private data class Entry(val id:String,val name:String,val category:String,val description:String,val kind:String,val source:String,val licence:String,val offline:Boolean,val installable:Boolean)
    private val entries = listOf(
        Entry("widget-host","Android Widget Host","Bureau","Afficher des widgets d'applications Android sur les bureaux LeafVibe.","Composant Android","API Android officielle","Android SDK",true,true),
        Entry("glance","Jetpack Glance","Bureau","Créer des widgets LeafVibe avec une approche Compose.","Bibliothèque Android","Android Developers","Apache-2.0",true,true),
        Entry("saf","Storage Access Framework","Fichiers","Accès aux fichiers et dossiers choisis explicitement dans Android.","Composant Android","API Android officielle","Android SDK",true,true),
        Entry("room","Room / SQLite","Données","Base locale structurée pour les extensions et les espaces LeafVibe.","Bibliothèque Android","Android Developers","Apache-2.0",true,true),
        Entry("work","WorkManager","Automatisation","Planifier les tâches différées qui doivent survivre aux redémarrages.","Bibliothèque Android","Android Developers","Apache-2.0",true,true),
        Entry("keystore","Android Keystore","Sécurité","Protéger les clés cryptographiques utilisées par LeafVibe.","Composant Android","API Android officielle","Android SDK",true,true),
        Entry("sqlcipher","SQLCipher","Sécurité","Étudier le chiffrement de bases SQLite locales.","Bibliothèque","github.com/sqlcipher","Licence à vérifier par version",true,true),
        Entry("syncthing","Syncthing","Synchronisation","Synchronisation de fichiers entre appareils sans serveur central obligatoire.","Application / connecteur externe","syncthing.net","MPL-2.0",true,false),
        Entry("automerge","Automerge","Synchronisation","Données structurées local-first avec résolution de conflits.","Bibliothèque","automerge.org","MIT",true,true),
        Entry("nodered","Node-RED","Automatisation","Éditeur visuel de flux et d'intégrations.","Service externe","nodered.org","Apache-2.0",false,false),
        Entry("homeassistant","Home Assistant","Automatisation","Connecter des équipements et automatisations locales.","Service / connecteur","home-assistant.io","Apache-2.0",true,false),
        Entry("kvaesitso","Kvaesitso","Inspiration","Référence open source pour recherche, lanceur et extensions.","Projet de référence","github.com/MM2-0/Kvaesitso","GPL-3.0 (à auditer)",true,false),
        Entry("lawnchair","Lawnchair","Inspiration","Référence pour l'interface launcher Android et les widgets.","Projet de référence","github.com/LawnchairLauncher/lawnchair","À vérifier selon composants",true,false),
        Entry("plasma","KDE Plasma Activities","Inspiration","Modèle d'activités, bureaux et panneaux pour LeafVibe.","Projet de référence","kde.org","Licences multiples",true,false)
    )
    private val bg=Color.rgb(10,18,25); private val panel=Color.rgb(24,39,49); private val green=Color.rgb(190,242,115); private val white=Color.WHITE; private val muted=Color.rgb(169,188,198)
    private val prefs by lazy { getSharedPreferences("leafvibe_extensions", MODE_PRIVATE) }
    private lateinit var content:LinearLayout
    private var tab="Catalogue"; private var category="Tout"; private var query=""
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun box(color:Int,radius:Int=18)=GradientDrawable().apply{setColor(color);cornerRadius=dp(radius).toFloat()}
    private fun label(value:String,size:Float,color:Int=white,bold:Boolean=false)=TextView(this).apply{text=value;textSize=size;setTextColor(color);if(bold)setTypeface(typeface,Typeface.BOLD);gravity=Gravity.CENTER_VERTICAL}
    private fun action(value:String,onClick:()->Unit)=TextView(this).apply{text=value;textSize=13f;setTextColor(white);gravity=Gravity.CENTER;setPadding(dp(12),dp(11),dp(12),dp(11));background=box(panel,13);setOnClickListener{onClick()}}
    private fun ids(key:String):MutableSet<String>=(prefs.getStringSet(key,emptySet())?:emptySet()).toMutableSet()
    private fun save(key:String,values:Set<String>){prefs.edit().putStringSet(key,values.toSet()).apply()}
    private fun installed(id:String)=id in ids("installed")
    private fun enabled(id:String)=id in ids("enabled")
    private fun favourite(id:String)=id in ids("favourites")
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);render()}
    private fun render(){
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(24),dp(16),dp(14));setBackgroundColor(bg)}
        val header=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
        val titleBox=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};titleBox.addView(label("Bibliothèque",25f,white,true));titleBox.addView(label("EXTENSIONS LEAFVIBE",10f,green,true))
        header.addView(titleBox,LinearLayout.LayoutParams(0,-2,1f));header.addView(action("← Bureau"){finish()});root.addView(header)
        root.addView(label("Découvre, ajoute et gère les outils de ton environnement.",12f,muted).apply{setPadding(0,dp(8),0,dp(12))})
        val stats=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        stats.addView(stat("CATALOGUE",entries.size.toString()),LinearLayout.LayoutParams(0,-2,1f));stats.addView(View(this),LinearLayout.LayoutParams(dp(7),1))
        stats.addView(stat("AJOUTÉES",ids("installed").size.toString()),LinearLayout.LayoutParams(0,-2,1f));stats.addView(View(this),LinearLayout.LayoutParams(dp(7),1))
        stats.addView(stat("ACTIVES",ids("enabled").size.toString()),LinearLayout.LayoutParams(0,-2,1f));root.addView(stats)
        val tabs=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(0,dp(12),0,dp(8))}
        listOf("Catalogue","Installées","Actives").forEach{v->tabs.addView(action(if(tab==v)"✓ $v" else v){tab=v;render()},LinearLayout.LayoutParams(0,-2,1f).apply{marginEnd=dp(4)})};root.addView(tabs)
        val search=EditText(this).apply{hint="Rechercher un outil…";setSingleLine(true);setText(query);setTextColor(white);setHintTextColor(muted);background=box(panel,13);setPadding(dp(13),dp(8),dp(13),dp(8));addTextChangedListener(object:TextWatcher{override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){};override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){query=s?.toString().orEmpty();fill()};override fun afterTextChanged(s:Editable?){}})}
        root.addView(search,LinearLayout.LayoutParams(-1,dp(48)))
        val cats=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(0,dp(7),0,dp(8))}
        listOf("Tout","Bureau","Fichiers","Données","Automatisation","Sécurité","Synchronisation","Inspiration").forEach{v->cats.addView(action(if(category==v)"• $v" else v){category=v;render()},LinearLayout.LayoutParams(-2,-2).apply{marginEnd=dp(5)})}
        root.addView(HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false;addView(cats)})
        val scroll=ScrollView(this);content=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};scroll.addView(content);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        root.addView(action("Désactiver toutes les extensions optionnelles"){save("enabled",emptySet());Toast.makeText(this,"Extensions optionnelles désactivées",Toast.LENGTH_SHORT).show();render()},LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(9)})
        setContentView(root);fill()
    }
    private fun stat(name:String,value:String)=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;background=box(panel,13);setPadding(dp(10),dp(8),dp(10),dp(8));addView(label(name,9f,muted,true));addView(label(value,19f,green,true))}
    private fun fill(){
        if(!::content.isInitialized)return
        content.removeAllViews()
        val visible=entries.filter{e->(when(tab){"Installées"->installed(e.id);"Actives"->enabled(e.id);else->true})&&(category=="Tout"||e.category==category)&&(query.isBlank()||(e.name+" "+e.description+" "+e.category).contains(query,true))}
        if(visible.isEmpty()){content.addView(label("Aucun outil dans cette vue.",14f,muted).apply{setPadding(dp(8),dp(22),dp(8),dp(22))});return}
        visible.forEach{content.addView(card(it))}
    }
    private fun card(e:Entry):View{
        val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;background=box(panel,17);setPadding(dp(13),dp(12),dp(13),dp(12))}
        val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
        top.addView(label(e.name,16f,white,true),LinearLayout.LayoutParams(0,-2,1f));top.addView(label(if(installed(e.id))"AJOUTÉ" else if(e.installable)"DISPONIBLE" else "RÉFÉRENCE",9f,if(installed(e.id))green else muted,true));card.addView(top)
        card.addView(label(e.category.uppercase()+"  •  "+e.kind,9f,green,true))
        card.addView(label(e.description,12f,white).apply{setPadding(0,dp(7),0,dp(7))})
        card.addView(label("Source : "+e.source,10f,muted));card.addView(label("Licence : "+e.licence+"  •  "+if(e.offline)"Hors ligne possible" else "Connexion requise",10f,muted))
        card.addView(label(if(e.installable)"L'action ajoute sa fiche au registre local. Le téléchargement et l'intégration réelle du composant restent à faire." else "Projet externe : référence uniquement. Installation et compatibilité à auditer avant intégration.",10f,muted).apply{setPadding(0,dp(5),0,dp(5))})
        val actions=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
        if(e.installable){
            if(!installed(e.id))actions.addView(action("＋ Ajouter"){val s=ids("installed");s.add(e.id);save("installed",s);render()},LinearLayout.LayoutParams(0,-2,1f))
            else{
                actions.addView(action(if(enabled(e.id))"Désactiver" else "Activer"){val s=ids("enabled");if(enabled(e.id))s.remove(e.id) else s.add(e.id);save("enabled",s);render()},LinearLayout.LayoutParams(0,-2,1f))
                actions.addView(action("Retirer"){val i=ids("installed");i.remove(e.id);save("installed",i);val a=ids("enabled");a.remove(e.id);save("enabled",a);render()},LinearLayout.LayoutParams(0,-2,1f).apply{marginStart=dp(6)})
            }
        }else actions.addView(action(if(favourite(e.id))"★ Favori" else "☆ Favori"){val s=ids("favourites");if(favourite(e.id))s.remove(e.id) else s.add(e.id);save("favourites",s);render()},LinearLayout.LayoutParams(0,-2,1f))
        card.addView(actions.apply{setPadding(0,dp(8),0,0)})
        return card.apply{layoutParams=LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(9)}}
    }
}
