package br.edu.odontoagora;

import android.app.*;
import android.content.*;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.util.Locale;

/** Screen coordinator for patient and dentist flows. All state changes are authorized by the API. */
public class MainActivity extends Activity {
    private Ui ui;
    private Session session;
    private ApiClient api;
    private LinearLayout content;
    private TextView error,toolbarTitle,countdown;
    private View loading;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private String screen="welcome",role="patient",lastStatus="",rendered="";
    private long callId,remainingDeadline;
    private int generation;
    private boolean busy,resumed;
    private JSONArray calls=new JSONArray();
    private JSONObject currentCall;
    private EditText email,password,name,city,clinic,address,cro,description;
    private SeekBar pain;
    private CheckBox swelling,bleeding;
    private String symptom="Dor intensa";
    private final int[] symptomIds={R.id.symptom_tooth,R.id.symptom_broken,R.id.symptom_blood,R.id.symptom_swelling};
    private final String[] symptoms={"Dor intensa","Dente quebrado","Sangramento","Inchaço"};
    private Runnable backAction;
    private long restoreId;

    private final Runnable poll=new Runnable(){ public void run(){
        if(!resumed)return;
        if(!busy){ if(screen.equals("call"))loadCall(false); else if(screen.equals("dashboard"))loadDashboard(false); }
        handler.postDelayed(this,5000);
    }};
    private final Runnable tick=new Runnable(){ public void run(){
        if(!resumed)return;
        if(countdown!=null && screen.equals("call")){
            long seconds=Math.max(0,(remainingDeadline-SystemClock.elapsedRealtime())/1000);
            countdown.setText(String.format(Locale.getDefault(),"%02d:%02d",seconds/60,seconds%60));
            if(seconds==0)countdown.setText("Prazo encerrado • atualizando…");
        }
        handler.postDelayed(this,1000);
    }};
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);setContentView(R.layout.activity_main);
        ui=new Ui(this);session=new Session(this);api=new ApiClient();
        content=findViewById(R.id.content);error=findViewById(R.id.error);loading=findViewById(R.id.loading);toolbarTitle=findViewById(R.id.toolbar_title);
        findViewById(R.id.back).setOnClickListener(v->{if(!busy&&backAction!=null)backAction.run();});
        findViewById(R.id.menu).setOnClickListener(v->{if(!busy)options();});
        if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);
        View root=findViewById(R.id.root);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30){ android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.ime()); v.setPadding(bars.left,bars.top,bars.right,bars.bottom); }
            return insets;
        });
        if(saved!=null){role=saved.getString("role","patient");restoreId=saved.getLong("callId");}
        if(session.token().isEmpty()){
            if(saved!=null && saved.getString("screen","").equals("register"))auth(true);
            else if(saved!=null && saved.getString("screen","").equals("login"))auth(false);else welcome();
        } else {
            page("loading","Sua conta",null);ui.title(content,"Bem-vindo de volta");ui.sub(content,"Recuperando seus atendimentos…");
            request("GET","/me",null,r->{session.user(r.optJSONObject("user"));
                if(saved!=null&&saved.getString("screen","").equals("triage")){triage();restoreDraft(saved);}
                else if(restoreId>0)openCall(restoreId);else dashboard();
            });
        }
    }
    @Override protected void onResume(){super.onResume();resumed=true;handler.post(poll);handler.post(tick);}
    @Override protected void onPause(){super.onPause();resumed=false;handler.removeCallbacks(poll);handler.removeCallbacks(tick);}
    @Override protected void onDestroy(){generation++;api.close();handler.removeCallbacksAndMessages(null);super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);state.putString("screen",screen);state.putString("role",role);state.putLong("callId",screen.equals("call")?callId:0);
        if(screen.equals("triage")){state.putString("city",value(city));state.putString("description",value(description));state.putString("symptom",symptom);state.putInt("pain",pain.getProgress());state.putBoolean("swelling",swelling.isChecked());state.putBoolean("bleeding",bleeding.isChecked());}
    }
    @Override public void onBackPressed(){if(!busy&&backAction!=null)backAction.run();else if(!busy)super.onBackPressed();}

    private interface Success {void done(JSONObject result);}
    private void request(String method,String path,JSONObject data,Success callback){
        if(busy)return;
        setBusy(true);int version=generation;
        api.request(session.base(),session.token(),method,path,data,(result,message,status)->{
            if(version!=generation)return;
            setBusy(false);
            if(message!=null){
                if(status==401&&!session.token().isEmpty()){session.clear();welcome();}
                showError(message);return;
            }
            error.setVisibility(View.GONE);callback.done(result);
        });
    }
    private void setBusy(boolean value){busy=value;loading.setVisibility(value?View.VISIBLE:View.GONE);enableButtons(content,!value);}
    private void enableButtons(View view,boolean enabled){
        if(view instanceof Button && !(view instanceof CompoundButton))view.setEnabled(enabled);
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)enableButtons(((ViewGroup)view).getChildAt(i),enabled);
    }
    private void showError(String message){error.setText(message);error.setVisibility(View.VISIBLE);}
    private void page(String id,String title,Runnable back){
        generation++;busy=false;loading.setVisibility(View.GONE);screen=id;rendered="";countdown=null;backAction=back;
        content.removeAllViews();error.setVisibility(View.GONE);toolbarTitle.setText(title);
        findViewById(R.id.back).setVisibility(back==null?View.INVISIBLE:View.VISIBLE);
        ((ScrollView)findViewById(R.id.scroll)).scrollTo(0,0);
    }
    private void welcome(){
        page("welcome","OdontoAgora",null);ui.space(content,22);
        ui.center(ui.text(content,"ATENDIMENTO ODONTOLÓGICO DE URGÊNCIA",11,ui.text,true));ui.space(content,18);
        ui.image(content,R.drawable.logo,170,"OdontoAgora");ui.space(content,14);
        ui.center(ui.text(content,"Quando a dor não\npode esperar.",26,ui.muted,true));ui.space(content,30);
        ui.button(content,"Sou paciente",true,()->{role="patient";auth(false);});
        ui.button(content,"Sou dentista",false,()->{role="dentist";auth(false);});
        ui.center(ui.sub(content,"Em sinais graves, procure um serviço hospitalar."));ui.space(content,20);
        ui.center(ui.sub(content,"Encontre um dentista disponível sem precisar ligar para várias clínicas."));
        ui.space(content,12);ui.center(ui.text(content,"Projeto acadêmico • utilize dados fictícios",12,ui.muted,false));
    }
    private void auth(boolean register){
        page(register?"register":"login",role.equals("patient")?"Área do paciente":"Área do dentista",this::welcome);
        ui.title(content,register?"Crie sua conta":"Que bom ter você aqui");
        ui.sub(content,register?"Preencha seus dados para começar.":"Entre para acompanhar seus atendimentos.");
        if(register)name=ui.input(content,"Nome completo","",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_WORDS,R.id.input_name);
        email=ui.input(content,"E-mail","",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,R.id.input_email);
        password=ui.input(content,"Senha (mínimo 8 caracteres)","",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD,R.id.input_password);
        password.setSaveEnabled(false);
        if(register){
            city=ui.input(content,"Cidade / UF","",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_WORDS,R.id.input_city);
            if(role.equals("dentist")){
                clinic=ui.input(content,"Nome da clínica","",InputType.TYPE_CLASS_TEXT,R.id.input_clinic);
                address=ui.input(content,"Endereço completo da clínica","",InputType.TYPE_CLASS_TEXT,R.id.input_address);
                cro=ui.input(content,"CRO / UF","",InputType.TYPE_CLASS_TEXT,R.id.input_cro);
                ui.sub(content,"No protótipo acadêmico, o CRO é informado e não possui verificação externa.");
            }
        }
        ui.button(content,register?"Criar conta":"Entrar",true,()->submitAuth(register));
        ui.button(content,register?"Já tenho conta":"Ainda não tenho conta",false,()->auth(!register));
        if(!register&&BuildConfig.DEBUG){
            ui.space(content,12);ui.sub(content,"Para apresentar em aula, use uma conta fictícia do servidor de demonstração.");
            ui.button(content,"Entrar na demonstração",false,()->{email.setText(role.equals("patient")?"paciente@demo.local":"dentista@demo.local");password.setText("Odonto123!");submitAuth(false);});
        }
    }
    private void submitAuth(boolean register){
        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(value(email)).matches()){email.setError("Informe um e-mail válido");return;}
        if(value(password).length()<8){password.setError("Use pelo menos 8 caracteres");return;}
        JSONObject data=json("email",value(email),"password",value(password));
        if(register){
            if(value(name).length()<2||value(city).length()<2){showError("Preencha seu nome e sua cidade.");return;}
            put(data,"name",value(name));put(data,"city",value(city));put(data,"role",role);
            if(role.equals("dentist")){put(data,"clinic",value(clinic));put(data,"address",value(address));put(data,"cro",value(cro));}
        }
        hideKeyboard();request("POST",register?"/auth/register":"/auth/login",data,r->{session.save(r);dashboard();});
    }
    private void dashboard(){
        callId=0;lastStatus="";page("dashboard","OdontoAgora",null);ui.title(content,"Seus atendimentos");ui.sub(content,"Carregando…");loadDashboard(true);
    }
    private void loadDashboard(boolean force){request("GET","/calls",null,r->{
        calls=r.optJSONArray("calls");if(calls==null)calls=new JSONArray();
        String snapshot=calls.toString()+session.user().toString();
        if(force||!snapshot.equals(rendered)){rendered=snapshot;renderDashboard();}
    });}
    private boolean dentist(){return session.user().optString("role").equals("dentist");}
    private void renderDashboard(){
        content.removeAllViews();JSONObject user=session.user();
        ui.text(content,dentist()?"ÁREA DO DENTISTA":"ÁREA DO PACIENTE",11,ui.muted,true);
        ui.title(content,"Olá, "+user.optString("name").split(" ")[0]);
        ui.sub(content,user.optString("city")+" • seus atendimentos em um só lugar");
        if(dentist()){
            boolean available=user.optInt("available")==1;
            ui.card(content,"DISPONIBILIDADE",available?"Plantão ativo":"Plantão pausado",available?"Você pode receber chamados da sua cidade.":"Ative o plantão quando puder atender.",available?ui.green:ui.yellow);
            ui.button(content,available?"Pausar plantão":"Ativar plantão",!available,()->request("POST","/me/availability",json("available",!available),r->{session.user(r.optJSONObject("user"));loadDashboard(true);}));
        }else{
            boolean active=false;for(int i=0;i<calls.length();i++)if(active(calls.optJSONObject(i).optString("status")))active=true;
            if(!active)ui.button(content,"Solicitar atendimento agora",true,this::triage);
        }
        ui.space(content,8);ui.text(content,dentist()?"FILA E HISTÓRICO":"SEUS CHAMADOS",12,ui.muted,true);
        if(calls.length()==0)ui.card(content,"TUDO EM DIA",dentist()?"Nenhum chamado por aqui":"Você ainda não tem chamados",dentist()?"Novos chamados aparecem automaticamente durante o plantão.":"Quando precisar, comece uma solicitação acima.",ui.text);
        for(int i=0;i<calls.length();i++){
            JSONObject call=calls.optJSONObject(i);String status=call.optString("status");
            LinearLayout card=ui.card(content,"#OA-"+call.optLong("id")+" • "+statusLabel(status),call.optString("symptom"),
                (dentist()?call.optString("patient_name")+" • ":"")+"Dor "+call.optInt("pain")+"/10 • "+call.optString("city"),statusColor(status));
            ui.button(card,"Ver atendimento",false,()->openCall(call.optLong("id")));
        }
        ui.button(content,"Atualizar chamados",false,()->loadDashboard(true));
    }
    private void triage(){
        page("triage","Relatar ocorrência",this::dashboard);symptom=symptoms[0];
        ui.title(content,"O que aconteceu?");ui.sub(content,"Escolha a opção mais próxima do que você sente.");
        RadioGroup group=new RadioGroup(this);group.setId(R.id.symptoms);group.setOrientation(LinearLayout.VERTICAL);
        int[] icons={R.drawable.tooth,R.drawable.broken,R.drawable.blood,R.drawable.swelling};
        for(int i=0;i<symptoms.length;i++){
            RadioButton option=new RadioButton(this);option.setId(symptomIds[i]);option.setText(symptoms[i]);option.setTextSize(15);option.setTextColor(ui.text);option.setMinHeight(ui.dp(64));
            option.setPadding(ui.dp(12),ui.dp(8),ui.dp(12),ui.dp(8));option.setBackgroundResource(R.drawable.card_background);
            android.graphics.drawable.Drawable icon=getDrawable(icons[i]);icon.setBounds(0,0,ui.dp(24),ui.dp(24));option.setCompoundDrawables(null,null,icon,null);
            RadioGroup.LayoutParams p=new RadioGroup.LayoutParams(-1,-2);p.bottomMargin=ui.dp(10);option.setLayoutParams(p);group.addView(option);
        }
        group.check(symptomIds[0]);group.setOnCheckedChangeListener((g,id)->{for(int i=0;i<symptomIds.length;i++)if(symptomIds[i]==id)symptom=symptoms[i];});content.addView(group);
        LinearLayout painCard=ui.card(content,"NÍVEL DA DOR","5 / 10","0 = sem dor • 10 = maior intensidade",ui.red);
        pain=new SeekBar(this);pain.setId(R.id.pain);pain.setMax(10);pain.setProgress(5);pain.setContentDescription("Intensidade da dor de zero a dez");pain.setMinimumHeight(ui.dp(48));painCard.addView(pain);
        pain.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int n,boolean from){((TextView)painCard.findViewById(R.id.card_value)).setText(n+" / 10");}});
        swelling=check("Tenho inchaço facial",R.id.swelling);bleeding=check("Tenho sangramento intenso",R.id.bleeding);
        city=ui.input(content,"Em qual cidade você está?",session.user().optString("city"),InputType.TYPE_CLASS_TEXT,R.id.triage_city);
        description=ui.input(content,"Conte mais (opcional, até 500 caracteres)","",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE,R.id.description);
        description.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(500)});
        ui.sub(content,"Em sinais graves, procure um serviço hospitalar. O aplicativo não realiza diagnóstico.");
        ui.button(content,"Buscar atendimento agora →",true,()->{
            if(value(city).length()<2){city.setError("Informe a cidade");return;}
            JSONObject data=json("symptom",symptom,"pain",pain.getProgress(),"swelling",swelling.isChecked(),"bleeding",bleeding.isChecked(),"city",value(city),"description",value(description));
            hideKeyboard();request("POST","/calls",data,r->openCall(r.optJSONObject("call").optLong("id")));
        });
    }
    private CheckBox check(String label,int id){CheckBox c=new CheckBox(this);c.setId(id);c.setText(label);c.setTextColor(ui.text);c.setTextSize(14);c.setMinHeight(ui.dp(48));c.setLayoutParams(ui.params(-2));content.addView(c);return c;}
    private void restoreDraft(Bundle state){city.setText(state.getString("city",""));description.setText(state.getString("description",""));pain.setProgress(state.getInt("pain",5));swelling.setChecked(state.getBoolean("swelling"));bleeding.setChecked(state.getBoolean("bleeding"));symptom=state.getString("symptom",symptoms[0]);for(int i=0;i<symptoms.length;i++)if(symptoms[i].equals(symptom))((RadioGroup)findViewById(R.id.symptoms)).check(symptomIds[i]);}
    private void openCall(long id){callId=id;lastStatus="";page("call","Atendimento #OA-"+id,this::dashboard);ui.sub(content,"Buscando informações do chamado…");loadCall(true);}
    private void loadCall(boolean force){request("GET","/calls/"+callId,null,r->{
        JSONObject call=r.optJSONObject("call");String status=call.optString("status");
        long seconds=Math.max(0,call.optLong("expires_at")-r.optLong("server_time"));remainingDeadline=SystemClock.elapsedRealtime()+seconds*1000;
        if(!dentist()&&lastStatus.equals("queued")&&status.equals("accepted")){
            Toast.makeText(this,"Um dentista aceitou seu chamado!",Toast.LENGTH_LONG).show();
            try{ToneGenerator tone=new ToneGenerator(AudioManager.STREAM_NOTIFICATION,70);tone.startTone(ToneGenerator.TONE_PROP_ACK,500);handler.postDelayed(tone::release,700);}catch(RuntimeException ignored){}
        }
        lastStatus=status;currentCall=call;
        if(force||!call.toString().equals(rendered)){rendered=call.toString();renderCall(call);}
    });}
    private void renderCall(JSONObject call){
        content.removeAllViews();countdown=null;String status=call.optString("status");boolean doctor=dentist();
        if(status.equals("queued")&&!doctor){
            ui.center(ui.text(content,"●  EM BUSCA",12,ui.yellow,true));ui.image(content,R.drawable.search_map,190,"Ilustração da busca por dentistas");
            ui.center(ui.text(content,"Ilustração • busca por cidade",11,ui.muted,false));
            ui.title(content,"Procurando dentista disponível");
            int count=call.optInt("available_dentists");ui.sub(content,count==0?"Ainda não há dentistas disponíveis nesta cidade. Você pode aguardar ou cancelar.":count+" profissional(is) disponível(is) em "+call.optString("city")+".");
            ui.card(content,"POSIÇÃO NA FILA",call.optInt("position")+"º", "Chamados são organizados por ordem de chegada.",ui.text);
            ui.card(content,"ETAPAS DA BUSCA","Chamado enviado","Dados recebidos pelo servidor. Aguardando o aceite de um dentista.",ui.green);
            ui.sub(content,"O tempo de espera depende da disponibilidade dos profissionais.");
        }else if(status.equals("queued")){
            ui.text(content,"PLANTÃO ATIVO",12,ui.green,true);ui.title(content,"Chamado urgente #OA-"+callId);
            ui.card(content,"PACIENTE",call.optString("patient_name"),call.optString("city"),ui.text);
        }else if(status.equals("accepted")){
            LinearLayout hero=ui.card(content,"ATENDIMENTO CONFIRMADO",doctor?"Vaga reservada":"Dentista encontrado!","Reserva válida por 45 minutos após o aceite.",ui.text);
            hero.setBackground(ui.shape(ui.brand,ui.brand));
            ((TextView)hero.findViewById(R.id.card_label)).setTextColor(ui.text);((TextView)hero.findViewById(R.id.card_detail)).setTextColor(ui.text);
            JSONObject d=call.optJSONObject("dentist");
            if(d!=null){ui.card(content,"DENTISTA",d.optString("name"),"CRO informado: "+d.optString("cro"),ui.text);ui.card(content,"CLÍNICA",d.optString("clinic"),d.optString("address"),ui.text);}
            LinearLayout timer=ui.card(content,doctor?"TEMPO PARA CHEGADA":"CHEGUE À CLÍNICA EM","45:00","A contagem continua mesmo com o aplicativo fechado.",ui.yellow);
            countdown=timer.findViewById(R.id.card_value);countdown.setTextSize(32);
            if(!doctor){ui.card(content,"CÓDIGO DE CHEGADA",call.optString("arrival_code"),"Mostre este código na recepção.",ui.text);ui.button(content,"Ver rota até a clínica →",true,()->route(call.optJSONObject("dentist")));}
        }else if(status.equals("in_care")){
            ui.text(content,"EM ATENDIMENTO",12,ui.green,true);ui.title(content,"Cuidado em andamento");ui.sub(content,doctor?"Registre o desfecho ao terminar o atendimento.":"O dentista confirmou sua chegada. Acompanhe o status por aqui.");
        }else if(status.equals("completed")||status.equals("return_needed")){
            ui.center(ui.text(content,"✓",56,ui.green,true));ui.title(content,status.equals("completed")?"Atendimento concluído":"Retorno indicado");
            ui.sub(content,"Chamado #OA-"+callId+" • "+call.optString("patient_name"));
            ui.card(content,"STATUS DO ATENDIMENTO",statusLabel(status),"Na fila → Aceito → Em atendimento → "+statusLabel(status),ui.green);
            ui.card(content,"DESFECHO",call.optString("outcome"),"Informação registrada pelo dentista responsável.",ui.text);
            if(call.optInt("return_days")>0)ui.card(content,"RETORNO RECOMENDADO","Em até "+call.optInt("return_days")+" dias","Entre em contato com a clínica para agendar.",ui.yellow);
        }else{
            ui.title(content,statusLabel(status));ui.card(content,"MOTIVO",call.optString("reason"),"Você pode iniciar uma nova solicitação na página inicial.",ui.yellow);
        }
        if(doctor||status.equals("queued")){
            ui.card(content,"RESUMO DA OCORRÊNCIA",call.optString("symptom"),"Dor: "+call.optInt("pain")+"/10\nInchaço facial: "+(call.optInt("swelling")==1?"Sim":"Não")+"\nSangramento intenso: "+(call.optInt("bleeding")==1?"Sim":"Não"),ui.red);
            if(!call.optString("description").isEmpty())ui.card(content,"RELATO DO PACIENTE",call.optString("description"),"",ui.text);
        }
        if(doctor){
            if(status.equals("queued")){
                ui.sub(content,"Ao aceitar, confirme que consegue receber o paciente nos próximos 45 minutos.");
                ui.button(content,"Aceitar atendimento →",true,()->action("accept",new JSONObject()));
                ui.button(content,"Não posso atender",false,()->request("POST","/calls/"+callId+"/decline",new JSONObject(),r->dashboard()));
            }else if(status.equals("accepted"))ui.button(content,"Confirmar chegada do paciente",true,this::arrival);
            else if(status.equals("in_care")){ui.button(content,"Concluir atendimento",true,()->outcome(false));ui.button(content,"Indicar retorno",false,()->outcome(true));}
            else if(status.equals("return_needed"))ui.button(content,"Concluir após retorno",true,()->outcome(false));
        }
        if(active(status)&&!(doctor&&status.equals("queued")))ui.button(content,"Cancelar chamado",false,this::cancel);
        ui.button(content,"Atualizar atendimento",false,()->loadCall(true));ui.button(content,"Voltar ao início",false,this::dashboard);
    }
    private void action(String endpoint,JSONObject data){request("POST","/calls/"+callId+"/"+endpoint,data,r->loadCall(true));}
    private void arrival(){
        EditText code=new EditText(this);code.setInputType(InputType.TYPE_CLASS_NUMBER);code.setHint("Código de 4 dígitos");
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Confirmar chegada").setMessage("Peça ao paciente o código exibido no aplicativo.").setView(code).setNegativeButton("Voltar",null).setPositiveButton("Confirmar",null).create();
        dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(w->{String value=code.getText().toString().trim();if(value.length()!=4){code.setError("Digite os 4 dígitos");return;}dialog.dismiss();action("status",json("status","in_care","arrival_code",value));}));dialog.show();
    }
    private void outcome(boolean needsReturn){
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(ui.dp(20),0,ui.dp(20),0);
        EditText note=ui.input(form,"Desfecho / orientação","",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE,R.id.outcome);
        EditText days=needsReturn?ui.input(form,"Retorno em quantos dias?","7",InputType.TYPE_CLASS_NUMBER,R.id.return_days):null;
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(needsReturn?"Indicar retorno":"Concluir atendimento").setView(form).setNegativeButton("Voltar",null).setPositiveButton("Salvar",null).create();
        dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(w->{
            if(value(note).length()<3){note.setError("Descreva o desfecho");return;}
            int n=0;if(needsReturn){try{n=Integer.parseInt(value(days));}catch(Exception e){n=0;}if(n<1||n>365){days.setError("Use de 1 a 365 dias");return;}}
            dialog.dismiss();action("status",json("status",needsReturn?"return_needed":"completed","outcome",value(note),"return_days",n));
        }));dialog.show();
    }
    private void cancel(){
        EditText reason=new EditText(this);reason.setHint("Informe o motivo (mínimo 5 caracteres)");reason.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Cancelar chamado?").setMessage("O motivo será registrado para o paciente e o dentista.").setView(reason).setNegativeButton("Manter chamado",null).setPositiveButton("Cancelar chamado",null).create();
        dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(w->{if(value(reason).length()<5){reason.setError("Explique o motivo");return;}dialog.dismiss();action("cancel",json("reason",value(reason)));}));dialog.show();
    }
    private void route(JSONObject dentist){
        if(dentist==null)return;Uri uri=Uri.parse("geo:0,0?q="+Uri.encode(dentist.optString("address")));
        try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(ActivityNotFoundException e){showError("Nenhum aplicativo de mapas instalado. Endereço: "+dentist.optString("address"));}
    }
    private void options(){
        String[] items=session.token().isEmpty()?new String[]{"Conexão com o servidor","Sobre o projeto"}:new String[]{"Atualizar","Sair da conta","Sobre o projeto"};
        new AlertDialog.Builder(this).setTitle("Opções").setItems(items,(d,n)->{
            if(items[n].equals("Conexão com o servidor"))connection();
            else if(items[n].equals("Sair da conta"))request("POST","/auth/logout",new JSONObject(),r->{session.clear();welcome();});
            else if(items[n].equals("Atualizar")){if(screen.equals("call"))loadCall(true);else dashboard();}
            else new AlertDialog.Builder(this).setTitle("OdontoAgora • MVP acadêmico").setMessage("Desenvolvimento Mobile • Yuri Aguiar Urbano\n\nJava + XML + API REST + SQLite.\n\nUse dados fictícios. O protótipo não realiza diagnóstico, não verifica CRO e não processa pagamentos. Atualizações automáticas ocorrem enquanto o aplicativo está aberto.").setPositiveButton("Entendi",null).show();
        }).show();
    }
    private void connection(){
        EditText url=new EditText(this);url.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);url.setText(session.base());
        AlertDialog d=new AlertDialog.Builder(this).setTitle("Conexão com o servidor").setMessage("No emulador: http://10.0.2.2:8080\nNo celular via USB: http://127.0.0.1:8080 (com adb reverse)").setView(url).setNegativeButton("Voltar",null).setPositiveButton("Salvar e testar",null).create();
        d.setOnShowListener(v->d.getButton(-1).setOnClickListener(w->{
            String base=value(url).replaceAll("/+$","");
            try{java.net.URI parsed=new java.net.URI(base);if(parsed.getHost()==null||parsed.getRawUserInfo()!=null||(!parsed.getScheme().equals("https")&&!(BuildConfig.DEBUG&&parsed.getScheme().equals("http"))))throw new Exception();}catch(Exception e){url.setError("Informe uma URL válida; HTTPS é obrigatório em produção.");return;}
            session.base(base);d.dismiss();request("GET","/health",null,r->Toast.makeText(this,"Servidor conectado",Toast.LENGTH_LONG).show());
        }));d.show();
    }
    private void hideKeyboard(){View focus=getCurrentFocus();if(focus!=null)((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(focus.getWindowToken(),0);}
    private static String value(EditText field){return field.getText().toString().trim();}
    static JSONObject json(Object... entries){JSONObject o=new JSONObject();for(int i=0;i<entries.length;i+=2)put(o,(String)entries[i],entries[i+1]);return o;}
    private static void put(JSONObject o,String k,Object v){try{o.put(k,v);}catch(JSONException e){throw new IllegalArgumentException(e);}}
    private static boolean active(String s){return s.equals("queued")||s.equals("accepted")||s.equals("in_care")||s.equals("return_needed");}
    private String statusLabel(String s){switch(s){case "queued":return "Na fila";case "accepted":return "Aceito";case "in_care":return "Em atendimento";case "return_needed":return "Precisa de retorno";case "completed":return "Concluído";case "cancelled":return "Cancelado";case "expired":return "Reserva expirada";default:return s;}}
    private int statusColor(String s){return s.equals("queued")||s.equals("expired")?ui.yellow:s.equals("cancelled")?ui.red:ui.green;}
}
