<h1 align="center">
  App ChronLog
</h1>

<p align="center">
  <strong>Repositório para centralizar o Ambiente de Desenvolvimento</strong>
  <p align="center">
    <img src="https://ci.appveyor.com/api/projects/status/g8d58ipi3auqdtrk/branch/master?svg=true" alt="Config. Device Activity Passing." />
     <!--<img src="https://ci.appveyor.com/api/projects/status/216h1g17b8ir009t?svg=true" alt="Config. Device Activity Crashing." /> -->
    <img src="https://img.shields.io/badge/version-final-blue.svg" alt="Current APP version." />  
  </p>
</p>

## 📋 Briefing

  Aplicativo para controle e ajustes no aparelho de termopar ChronTeck.


## 📖 Requirements
```
    implementation fileTree(dir: 'libs', include: ['*.jar'])
    implementation 'androidx.appcompat:appcompat:1.1.0'
    implementation 'androidx.constraintlayout:constraintlayout:1.1.3'
    implementation 'androidx.annotation:annotation:1.1.0'
    implementation 'androidx.lifecycle:lifecycle-extensions:2.1.0'
    implementation 'androidx.legacy:legacy-support-v4:1.0.0'
    testImplementation 'junit:junit:4.12'
    androidTestImplementation 'androidx.test.ext:junit:1.1.1'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.2.0'

    implementation 'androidx.core:core-ktx:1.2.0-rc01'

    implementation "com.google.android.material:material:1.2.0-alpha02"
    implementation 'com.android.support:support-v4:29.0.0-alpha1'
    implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
    implementation 'com.google.code.gson:gson:2.8.6'
    //implementation 'br.com.simplepass:loading-button-android:2.2.0'

    implementation("com.google.guava:guava:28.1-android")

```

## 🚀 ScreensShots

## 👏 Todo (Desenvolvimento)

- [x] Criar repositório no Github
- [x] Criar SplashScreen (Android)
- [x] Desenvolver Tela Serial (check User Active)

* ~Desenvolver Tela de Serial ["Login"]~

  ~- [ ] Lógica Própria para Device~
  
* Desenvolver Tela de Conexão Bluetooth
  - [x] Interface Bluetooth serial (modo SSP)
  - [x] Refresh Lista de Dispositivos
  - [x] Auto-Conexão [com pin '1234']
  - [x] Conexão ao Dispositivo
  
* Desenvolver Tela Configurações
  - [x] Botão "Parear Hora"
  - [x] Botão "Parear Data"
  - [x] Dropbox "Setar Termopar"
  - [x] Input + Botão "Configurar Tempo de Aquisição"
  
* Desenvolver Tela Chart 
  - [x] Chart com valores estáticos
  - [x] Chart com valores dinâmicos
  - [x] Chart com filtro de Termopares
  
* Desenvolver tela Termostato
  - [x] Salvar arquivo
  - [x] Listar Arquivos
  - [x] Deletar Arquivos
  - [x] Conexão com Termostato
  - [x] Resgatar dados do Termostato
  - [x] Implementar Compartilhamento de Dados Resgatados

## How to version

Versionamento será dividido entre

- Mudanças significativas de funcionalidade do App (+x.0.0)
- Adição de novas funcionalidades (0.+x.0)
- Ajustes de Bugs (0.0.+x)

#### Exemplo:

> Foram adicionadas 3 novas telas, 5 novas funcionalidades e corrigidos 15 bugs. Logo a versão continuará 1, porém com 8 incrementos de funcionalidades e 15 correções de bugs. Versão final: 1.8.15

#### 👏 Todo (README.MD)

- [ ] Implementar ScreensShots no README.MD
- [x] Adicionar Dependências
- [x] Incrementar Todo(Dev)
