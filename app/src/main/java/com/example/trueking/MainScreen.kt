package com.example.trueking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import android.content.Context
import kotlinx.coroutines.launch

// Modelo de datos para los items de trueque
data class TruequeItem(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val tipo: TipoTrueque,
    val usuario: String,
    val usuarioId: String = "",
    val categoria: String = "Otro",
    val imagenUrl: String? = null
)

data class SolicitudTrueque(
    val id: String,
    val truequeSolicitadoId: String,
    val truequeSolicitadoTitulo: String,
    val truequeOfrecidoId: String,
    val truequeOfrecidoTitulo: String,
    val solicitanteId: String,
    val solicitanteNombre: String,
    val propietarioId: String,
    val estado: String
)

enum class TipoTrueque {
    OBJETO, HABILIDAD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaNotificaciones(
    onVolver: () -> Unit,
    solicitudes: List<SolicitudTrueque>,
    onAceptar: (SolicitudTrueque) -> Unit,
    onRechazar: (SolicitudTrueque) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Solicitudes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (solicitudes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No tienes solicitudes pendientes.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(solicitudes) { solicitud ->
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${solicitud.solicitanteNombre} te ha hecho una solicitud",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Te pide: ${solicitud.truequeSolicitadoTitulo}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "A cambio ofrece: ${solicitud.truequeOfrecidoTitulo}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = { onAceptar(solicitud) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Aceptar")
                                }
                                OutlinedButton(
                                    onClick = { onRechazar(solicitud) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Rechazar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class RutaPantalla {
    PRINCIPAL,
    PERFIL,
    AGREGAR_TRUEQUE,
    LOGIN,
    REGISTRO,
    NOTIFICACIONES
}

@Composable
fun AppTrueque() {
    var rutaActual by rememberSaveable { mutableStateOf(RutaPantalla.LOGIN) }
    var rutaVolverAgregar by rememberSaveable { mutableStateOf(RutaPantalla.PRINCIPAL) }
    var rutaVolverNotificaciones by rememberSaveable { mutableStateOf(RutaPantalla.PRINCIPAL) }
    val misTrueques = remember { mutableStateListOf<TruequeItem>() }
    val truequesGlobales = remember { mutableStateListOf<TruequeItem>() }
    val solicitudesPendientes = remember { mutableStateListOf<SolicitudTrueque>() }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE) }
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    var uidActual by remember { mutableStateOf(auth.currentUser?.uid ?: "") }
    var nombreUsuarioActual by remember { mutableStateOf("") }

    val misTruequesParaUI by remember {
        derivedStateOf {
            val nombre = nombreUsuarioActual.trim()
            val sinId = if (nombre.isBlank()) {
                emptyList()
            } else {
                truequesGlobales.filter { it.usuarioId.isBlank() && it.usuario.equals(nombre, ignoreCase = true) }
            }
            (misTrueques.toList() + sinId).distinctBy { it.id }
        }
    }

    DisposableEffect(Unit) {
        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            uidActual = firebaseAuth.currentUser?.uid ?: ""
        }
        auth.addAuthStateListener(authListener)
        onDispose { auth.removeAuthStateListener(authListener) }
    }

    DisposableEffect(uidActual) {
        var registro: ListenerRegistration? = null
        if (uidActual.isBlank()) {
            nombreUsuarioActual = ""
        } else {
            registro = db.collection("usuarios").document(uidActual)
                .addSnapshotListener { snapshot, _ ->
                    nombreUsuarioActual = snapshot?.getString("nombre") ?: ""
                }
        }

        onDispose { registro?.remove() }
    }

    DisposableEffect(Unit) {
        val registro: ListenerRegistration = db.collection("trueques")
            .orderBy("creadoEn", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                truequesGlobales.clear()
                truequesGlobales.addAll(
                    snapshot.documents.mapNotNull { doc ->
                        val titulo = doc.getString("titulo") ?: return@mapNotNull null
                        val descripcion = doc.getString("descripcion") ?: ""
                        val tipoStr = doc.getString("tipo") ?: TipoTrueque.OBJETO.name
                        val usuario = doc.getString("usuario") ?: ""
                        val usuarioId = doc.getString("usuarioId") ?: ""
                        val categoria = doc.getString("categoria") ?: "Otro"

                        TruequeItem(
                            id = doc.id,
                            titulo = titulo,
                            descripcion = descripcion,
                            tipo = if (tipoStr == TipoTrueque.HABILIDAD.name) TipoTrueque.HABILIDAD else TipoTrueque.OBJETO,
                            usuario = usuario,
                            usuarioId = usuarioId,
                            categoria = categoria
                        )
                    }
                )
            }

        onDispose { registro.remove() }
    }

    DisposableEffect(uidActual) {
        var registro: ListenerRegistration? = null
        if (uidActual.isBlank()) {
            misTrueques.clear()
        } else {
            registro = db.collection("trueques")
                .whereEqualTo("usuarioId", uidActual)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot == null) return@addSnapshotListener
                    misTrueques.clear()
                    misTrueques.addAll(
                        snapshot.documents.mapNotNull { doc ->
                            val titulo = doc.getString("titulo") ?: return@mapNotNull null
                            val descripcion = doc.getString("descripcion") ?: ""
                            val tipoStr = doc.getString("tipo") ?: TipoTrueque.OBJETO.name
                            val usuario = doc.getString("usuario") ?: ""
                            val usuarioId = doc.getString("usuarioId") ?: ""
                            val categoria = doc.getString("categoria") ?: "Otro"

                            TruequeItem(
                                id = doc.id,
                                titulo = titulo,
                                descripcion = descripcion,
                                tipo = if (tipoStr == TipoTrueque.HABILIDAD.name) TipoTrueque.HABILIDAD else TipoTrueque.OBJETO,
                                usuario = usuario,
                                usuarioId = usuarioId,
                                categoria = categoria
                            )
                        }
                    )
                }
        }

        onDispose { registro?.remove() }
    }

    DisposableEffect(uidActual) {
        var registro: ListenerRegistration? = null
        if (uidActual.isBlank()) {
            solicitudesPendientes.clear()
        } else {
            registro = db.collection("solicitudes")
                .whereEqualTo("propietarioId", uidActual)
                .whereEqualTo("estado", "pendiente")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot == null) return@addSnapshotListener
                    solicitudesPendientes.clear()
                    solicitudesPendientes.addAll(
                        snapshot.documents.mapNotNull { doc ->
                            val truequeSolicitadoId = doc.getString("truequeSolicitadoId") ?: return@mapNotNull null
                            val truequeSolicitadoTitulo = doc.getString("truequeSolicitadoTitulo") ?: ""
                            val truequeOfrecidoId = doc.getString("truequeOfrecidoId") ?: return@mapNotNull null
                            val truequeOfrecidoTitulo = doc.getString("truequeOfrecidoTitulo") ?: ""
                            val solicitanteId = doc.getString("solicitanteId") ?: ""
                            val solicitanteNombre = doc.getString("solicitanteNombre") ?: ""
                            val propietarioId = doc.getString("propietarioId") ?: ""
                            val estado = doc.getString("estado") ?: "pendiente"

                            SolicitudTrueque(
                                id = doc.id,
                                truequeSolicitadoId = truequeSolicitadoId,
                                truequeSolicitadoTitulo = truequeSolicitadoTitulo,
                                truequeOfrecidoId = truequeOfrecidoId,
                                truequeOfrecidoTitulo = truequeOfrecidoTitulo,
                                solicitanteId = solicitanteId,
                                solicitanteNombre = solicitanteNombre,
                                propietarioId = propietarioId,
                                estado = estado
                            )
                        }
                    )
                }
        }

        onDispose { registro?.remove() }
    }

    LaunchedEffect(Unit) {
        val rememberMe = prefs.getBoolean("remember_me", false)
        val user = auth.currentUser
        rutaActual = if (user != null && rememberMe) {
            RutaPantalla.PRINCIPAL
        } else {
            RutaPantalla.LOGIN
        }
    }

    when (rutaActual) {
        RutaPantalla.PRINCIPAL -> PantallaPrincipal(
            onPerfilClick = { rutaActual = RutaPantalla.PERFIL },
            onNuevoTruequeClick = {
                rutaVolverAgregar = RutaPantalla.PRINCIPAL
                rutaActual = RutaPantalla.AGREGAR_TRUEQUE
            },
            trueques = truequesGlobales,
            misTruequesUsuario = misTruequesParaUI,
            solicitudesPendientesCount = solicitudesPendientes.size,
            onNotificacionesClick = {
                rutaVolverNotificaciones = RutaPantalla.PRINCIPAL
                rutaActual = RutaPantalla.NOTIFICACIONES
            },
            onCrearSolicitud = { truequeSolicitado, truequeOfrecido ->
                if (uidActual.isBlank()) return@PantallaPrincipal
                if (truequeSolicitado.usuarioId == uidActual) return@PantallaPrincipal

                val datos = mapOf(
                    "truequeSolicitadoId" to truequeSolicitado.id,
                    "truequeSolicitadoTitulo" to truequeSolicitado.titulo,
                    "truequeOfrecidoId" to truequeOfrecido.id,
                    "truequeOfrecidoTitulo" to truequeOfrecido.titulo,
                    "solicitanteId" to uidActual,
                    "solicitanteNombre" to (if (nombreUsuarioActual.isNotBlank()) nombreUsuarioActual else "Usuario"),
                    "propietarioId" to truequeSolicitado.usuarioId,
                    "estado" to "pendiente",
                    "creadoEn" to FieldValue.serverTimestamp()
                )
                db.collection("solicitudes").add(datos)
            }
        )

        RutaPantalla.PERFIL -> PantallaPerfil(
            onVolver = { rutaActual = RutaPantalla.PRINCIPAL },
            misTrueques = misTruequesParaUI,
            onAgregarTrueque = {
                rutaVolverAgregar = RutaPantalla.PERFIL
                rutaActual = RutaPantalla.AGREGAR_TRUEQUE
            },
            onCerrarSesion = {
                prefs.edit().putBoolean("remember_me", false).apply()
                FirebaseAuth.getInstance().signOut()
                rutaActual = RutaPantalla.LOGIN
            },
            solicitudesPendientesCount = solicitudesPendientes.size,
            onNotificacionesClick = {
                rutaVolverNotificaciones = RutaPantalla.PERFIL
                rutaActual = RutaPantalla.NOTIFICACIONES
            }
        )

        RutaPantalla.AGREGAR_TRUEQUE -> PantallaAgregarTrueque(
            onVolver = { rutaActual = rutaVolverAgregar },
            onGuardar = { titulo, descripcion, tipo, categoria ->
                if (uidActual.isNotBlank()) {
                    val datos = mapOf(
                        "titulo" to titulo.trim(),
                        "descripcion" to descripcion.trim(),
                        "tipo" to tipo.name,
                        "usuario" to (if (nombreUsuarioActual.isNotBlank()) nombreUsuarioActual else "Usuario"),
                        "usuarioId" to uidActual,
                        "categoria" to categoria,
                        "creadoEn" to FieldValue.serverTimestamp()
                    )
                    db.collection("trueques").add(datos)
                }
                rutaActual = RutaPantalla.PERFIL
            }
        )

        RutaPantalla.LOGIN -> PantallaLogin(
            onLoginExitoso = { rememberMe ->
                prefs.edit().putBoolean("remember_me", rememberMe).apply()
                rutaActual = RutaPantalla.PRINCIPAL
            },
            onIrARegistro = { rutaActual = RutaPantalla.REGISTRO }
        )

        RutaPantalla.REGISTRO -> PantallaRegistro(
            onRegistroExitoso = { rememberMe ->
                prefs.edit().putBoolean("remember_me", rememberMe).apply()
                rutaActual = RutaPantalla.PRINCIPAL
            },
            onVolverLogin = { rutaActual = RutaPantalla.LOGIN }
        )

        RutaPantalla.NOTIFICACIONES -> PantallaNotificaciones(
            onVolver = { rutaActual = rutaVolverNotificaciones },
            solicitudes = solicitudesPendientes,
            onAceptar = { solicitud ->
                val refSolicitud = db.collection("solicitudes").document(solicitud.id)
                val refSolicitado = db.collection("trueques").document(solicitud.truequeSolicitadoId)
                val refOfrecido = db.collection("trueques").document(solicitud.truequeOfrecidoId)
                db.runBatch { batch ->
                    batch.delete(refSolicitado)
                    batch.delete(refOfrecido)
                    batch.update(refSolicitud, mapOf("estado" to "aceptada"))
                }
            },
            onRechazar = { solicitud ->
                db.collection("solicitudes").document(solicitud.id)
                    .update(mapOf("estado" to "rechazada"))
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipal(
    onPerfilClick: () -> Unit,
    onNuevoTruequeClick: () -> Unit,
    trueques: List<TruequeItem>,
    misTruequesUsuario: List<TruequeItem>,
    solicitudesPendientesCount: Int,
    onNotificacionesClick: () -> Unit,
    onCrearSolicitud: (TruequeItem, TruequeItem) -> Unit
) {

    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var categoriaSeleccionada by rememberSaveable { mutableStateOf<String?>(null) }
    var itemDetalle by remember { mutableStateOf<TruequeItem?>(null) }
    val favoritos = remember { mutableStateListOf<String>() }
    val snackbarHostState = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()
    var itemSolicitud by remember { mutableStateOf<TruequeItem?>(null) }
    var truequeOfrecidoSeleccionadoId by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "TruequeApp",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    IconButton(onClick = onNotificacionesClick) {
                        BadgedBox(
                            badge = {
                                if (solicitudesPendientesCount > 0) {
                                    Badge { Text(solicitudesPendientesCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, "Notificaciones")
                        }
                    }
                    IconButton(onClick = onPerfilClick) {
                        Icon(Icons.Default.Person, "Perfil")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNuevoTruequeClick,
                icon = { Icon(Icons.Default.Add, "Añadir") },
                text = { Text("Nuevo Trueque") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Barra de búsqueda
            SearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                modifier = Modifier.padding(16.dp)
            )

            // Tabs para filtrar
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Todos") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Objetos") },
                    icon = { Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(20.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Habilidades") },
                    icon = { Icon(Icons.Default.Star, null, modifier = Modifier.size(20.dp)) }
                )
            }

            // Categorías destacadas
            CategoriesSection(
                categoriaSeleccionada = categoriaSeleccionada,
                onCategoriaSeleccionadaChange = { nueva -> categoriaSeleccionada = nueva }
            )

            // Lista de items
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val itemsToShow = when (selectedTab) {
                    1 -> trueques.filter { it.tipo == TipoTrueque.OBJETO }
                    2 -> trueques.filter { it.tipo == TipoTrueque.HABILIDAD }
                    else -> trueques
                }

                val itemsFiltrados = itemsToShow
                    .asSequence()
                    .filter { item ->
                        if (searchQuery.isBlank()) {
                            true
                        } else {
                            val q = searchQuery.trim()
                            item.titulo.contains(q, ignoreCase = true) ||
                                item.descripcion.contains(q, ignoreCase = true) ||
                                item.usuario.contains(q, ignoreCase = true)
                        }
                    }
                    .filter { item ->
                        when (categoriaSeleccionada) {
                            null -> true
                            else -> item.categoria.equals(categoriaSeleccionada, ignoreCase = true)
                        }
                    }
                    .toList()

                items(itemsFiltrados) { item ->
                    TruequeCard(
                        item = item,
                        onClick = { itemDetalle = item },
                        onTipoClick = {
                            selectedTab = if (item.tipo == TipoTrueque.OBJETO) 1 else 2
                        }
                    )
                }
            }
        }
    }

    itemDetalle?.let { item ->
        val esFavorito = favoritos.contains(item.id)
        AlertDialog(
            onDismissRequest = { itemDetalle = null },
            title = { Text(item.titulo, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.descripcion)
                    Text(
                        text = "Usuario: ${item.usuario}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            if (esFavorito) {
                                favoritos.remove(item.id)
                            } else {
                                favoritos.add(item.id)
                            }
                            alcance.launch {
                                snackbarHostState.showSnackbar(
                                    if (esFavorito) "Eliminado de favoritos." else "Añadido a favoritos."
                                )
                            }
                        }
                    ) {
                        Text(if (esFavorito) "Quitar favorito" else "Favorito")
                    }

                    Button(
                        onClick = {
                            itemDetalle = null
                            if (misTruequesUsuario.isEmpty()) {
                                alcance.launch { snackbarHostState.showSnackbar("Primero crea un trueque para poder ofrecerlo.") }
                                return@Button
                            }
                            itemSolicitud = item
                        }
                    ) {
                        Text("Solicitar")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { itemDetalle = null }) {
                    Text("Cerrar")
                }
            }
        )
    }

    itemSolicitud?.let { solicitado ->
        AlertDialog(
            onDismissRequest = {
                itemSolicitud = null
                truequeOfrecidoSeleccionadoId = null
            },
            title = { Text("Selecciona qué ofreces a cambio") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Te interesa: ${solicitado.titulo}")
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(misTruequesUsuario) { miTrueque ->
                            val seleccionado = truequeOfrecidoSeleccionadoId == miTrueque.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { truequeOfrecidoSeleccionadoId = miTrueque.id },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (seleccionado) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(miTrueque.titulo, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        miTrueque.categoria,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idSeleccionado = truequeOfrecidoSeleccionadoId
                        val ofrecido = misTruequesUsuario.firstOrNull { it.id == idSeleccionado }
                        if (ofrecido == null) {
                            alcance.launch { snackbarHostState.showSnackbar("Selecciona un trueque para ofrecer.") }
                            return@Button
                        }
                        onCrearSolicitud(solicitado, ofrecido)
                        itemSolicitud = null
                        truequeOfrecidoSeleccionadoId = null
                        alcance.launch { snackbarHostState.showSnackbar("Solicitud enviada.") }
                    }
                ) {
                    Text("Enviar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        itemSolicitud = null
                        truequeOfrecidoSeleccionadoId = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAgregarTrueque(
    onVolver: () -> Unit,
    onGuardar: (String, String, TipoTrueque, String) -> Unit
) {
    var titulo by rememberSaveable { mutableStateOf("") }
    var descripcion by rememberSaveable { mutableStateOf("") }
    var tipo by rememberSaveable { mutableStateOf(TipoTrueque.OBJETO) }
    var categoria by rememberSaveable { mutableStateOf("Otro") }
    var mostrarError by rememberSaveable { mutableStateOf(false) }

    val formularioValido = titulo.isNotBlank() && descripcion.isNotBlank() && categoria.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Nuevo trueque", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = titulo,
                onValueChange = {
                    titulo = it
                    if (mostrarError) mostrarError = false
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Título") },
                singleLine = true
            )

            OutlinedTextField(
                value = descripcion,
                onValueChange = {
                    descripcion = it
                    if (mostrarError) mostrarError = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                label = { Text("Descripción") }
            )

            Text(
                text = "Tipo de trueque",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = tipo == TipoTrueque.OBJETO,
                    onClick = { tipo = TipoTrueque.OBJETO },
                    label = { Text("Objeto") }
                )
                FilterChip(
                    selected = tipo == TipoTrueque.HABILIDAD,
                    onClick = { tipo = TipoTrueque.HABILIDAD },
                    label = { Text("Habilidad") }
                )
            }

            Text(
                text = "Categoría",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            val categorias = listOf("Electrónica", "Libros", "Deportes", "Música", "Idiomas", "Otro")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(categorias) { cat ->
                    FilterChip(
                        selected = categoria == cat,
                        onClick = {
                            categoria = cat
                            if (mostrarError) mostrarError = false
                        },
                        label = { Text(cat) }
                    )
                }
            }

            if (mostrarError) {
                Text(
                    text = "Completa el título, la descripción y la categoría.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (!formularioValido) {
                        mostrarError = true
                        return@Button
                    }
                    onGuardar(titulo.trim(), descripcion.trim(), tipo, categoria)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Publicar trueque")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPerfil(
    onVolver: () -> Unit,
    misTrueques: List<TruequeItem>,
    onAgregarTrueque: () -> Unit,
    onCerrarSesion: () -> Unit,
    solicitudesPendientesCount: Int,
    onNotificacionesClick: () -> Unit
) {
    var nombreUsuario by remember { mutableStateOf("") }
    var usuario by remember { mutableStateOf("") }
    var mostrarDialogoEditarNombre by rememberSaveable { mutableStateOf(false) }
    var nombreEditado by rememberSaveable { mutableStateOf("") }
    var mostrarDialogoConfirmarEliminar by rememberSaveable { mutableStateOf(false) }
    var truequeAEliminar by remember { mutableStateOf<TruequeItem?>(null) }
    val ubicacion = "Madrid"
    val truequesRealizados = 18
    val truequesActivos = misTrueques.size
    val truequesFavoritos = 12

    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val snackbarHostState = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("usuarios").document(uid).get()
                .addOnSuccessListener { snap ->
                    nombreUsuario = snap.getString("nombre") ?: ""
                    val usuarioStr = snap.getString("usuario") ?: ""
                    usuario = if (usuarioStr.startsWith("@")) usuarioStr else if (usuarioStr.isNotBlank()) "@$usuarioStr" else ""
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Perfil", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onNotificacionesClick) {
                        BadgedBox(
                            badge = {
                                if (solicitudesPendientesCount > 0) {
                                    Badge { Text(solicitudesPendientesCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, "Notificaciones")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(72.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = nombreUsuario,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = usuario,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = ubicacion,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    nombreEditado = nombreUsuario
                                    mostrarDialogoEditarNombre = true
                                }
                            ) {
                                Text("Editar")
                            }
                        }

                        Button(
                            onClick = onAgregarTrueque,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Añadir trueque")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = truequesActivos.toString(),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Activos",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = truequesRealizados.toString(),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "Realizados",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = truequesFavoritos.toString(),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = "Favoritos",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Mis trueques",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (misTrueques.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Aún no has publicado trueques.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Pulsa en \"Añadir trueque\" para crear el primero.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(misTrueques) { itemTrueque ->
                    TruequeCard(
                        item = itemTrueque,
                        onClick = { },
                        onTipoClick = { },
                        onEliminar = {
                            truequeAEliminar = itemTrueque
                            mostrarDialogoConfirmarEliminar = true
                        }
                    )
                }
            }

            item {
                Button(
                    onClick = onCerrarSesion,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cerrar sesión")
                }
            }
        }
    }

    if (mostrarDialogoEditarNombre) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEditarNombre = false },
            title = { Text("Editar nombre") },
            text = {
                OutlinedTextField(
                    value = nombreEditado,
                    onValueChange = { nombreEditado = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uid = auth.currentUser?.uid
                        val nuevoNombre = nombreEditado.trim()
                        if (uid.isNullOrBlank() || nuevoNombre.isBlank()) {
                            alcance.launch { snackbarHostState.showSnackbar("Introduce un nombre válido.") }
                            return@Button
                        }
                        db.collection("usuarios").document(uid)
                            .update(mapOf("nombre" to nuevoNombre))
                            .addOnSuccessListener {
                                nombreUsuario = nuevoNombre
                                mostrarDialogoEditarNombre = false
                                alcance.launch { snackbarHostState.showSnackbar("Nombre actualizado.") }
                            }
                            .addOnFailureListener { e ->
                                alcance.launch {
                                    snackbarHostState.showSnackbar(
                                        "No se pudo actualizar el nombre" +
                                            (e.localizedMessage?.let { ": $it" } ?: "")
                                    )
                                }
                            }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoEditarNombre = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (mostrarDialogoConfirmarEliminar) {
        val item = truequeAEliminar
        AlertDialog(
            onDismissRequest = {
                mostrarDialogoConfirmarEliminar = false
                truequeAEliminar = null
            },
            title = { Text("Eliminar trueque") },
            text = { Text("¿Seguro que quieres eliminar este trueque?") },
            confirmButton = {
                Button(
                    onClick = {
                        if (item == null) {
                            mostrarDialogoConfirmarEliminar = false
                            return@Button
                        }
                        db.collection("trueques").document(item.id).delete()
                            .addOnSuccessListener {
                                mostrarDialogoConfirmarEliminar = false
                                truequeAEliminar = null
                                alcance.launch { snackbarHostState.showSnackbar("Trueque eliminado.") }
                            }
                            .addOnFailureListener { e ->
                                mostrarDialogoConfirmarEliminar = false
                                truequeAEliminar = null
                                alcance.launch {
                                    snackbarHostState.showSnackbar(
                                        "No se pudo eliminar el trueque" +
                                            (e.localizedMessage?.let { ": $it" } ?: "")
                                    )
                                }
                            }
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoConfirmarEliminar = false
                        truequeAEliminar = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaLogin(
    onLoginExitoso: (rememberMe: Boolean) -> Unit,
    onIrARegistro: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    var correo by rememberSaveable { mutableStateOf("") }
    var contrasena by rememberSaveable { mutableStateOf("") }
    var recordar by rememberSaveable { mutableStateOf(true) }
    var cargando by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Iniciar sesión", fontWeight = FontWeight.Bold) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = correo,
                onValueChange = { correo = it },
                label = { Text("Correo electrónico") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = contrasena,
                onValueChange = { contrasena = it },
                label = { Text("Contraseña") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = recordar, onCheckedChange = { recordar = it })
                Spacer(modifier = Modifier.width(8.dp))
                Text("Recuérdame")
            }
            Button(
                onClick = {
                    if (correo.isBlank() || contrasena.isBlank()) {
                        alcance.launch { snackbarHostState.showSnackbar("Introduce correo y contraseña.") }
                        return@Button
                    }
                    cargando = true
                    auth.signInWithEmailAndPassword(correo.trim(), contrasena)
                        .addOnCompleteListener { task ->
                            cargando = false
                            if (task.isSuccessful) {
                                onLoginExitoso(recordar)
                            } else {
                                alcance.launch { snackbarHostState.showSnackbar(task.exception?.localizedMessage ?: "Error de autenticación") }
                            }
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !cargando
            ) {
                if (cargando) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Entrar")
            }
            TextButton(onClick = onIrARegistro, modifier = Modifier.fillMaxWidth()) {
                Text("¿No tienes cuenta? Regístrate")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaRegistro(
    onRegistroExitoso: (rememberMe: Boolean) -> Unit,
    onVolverLogin: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    var nombre by rememberSaveable { mutableStateOf("") }
    var usuario by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var contrasena by rememberSaveable { mutableStateOf("") }
    var recordar by rememberSaveable { mutableStateOf(true) }
    var cargando by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear cuenta", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVolverLogin) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = usuario,
                onValueChange = { usuario = it },
                label = { Text("Usuario") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = correo,
                onValueChange = { correo = it },
                label = { Text("Correo electrónico") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = contrasena,
                onValueChange = { contrasena = it },
                label = { Text("Contraseña") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = recordar, onCheckedChange = { recordar = it })
                Spacer(modifier = Modifier.width(8.dp))
                Text("Recuérdame")
            }
            Button(
                onClick = {
                    if (nombre.isBlank() || usuario.isBlank() || correo.isBlank() || contrasena.isBlank()) {
                        alcance.launch { snackbarHostState.showSnackbar("Completa todos los campos.") }
                        return@Button
                    }
                    cargando = true
                    auth.createUserWithEmailAndPassword(correo.trim(), contrasena)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val uid = task.result?.user?.uid
                                if (uid != null) {
                                    val datos = mapOf(
                                        "nombre" to nombre.trim(),
                                        "usuario" to usuario.trim(),
                                        "correo" to correo.trim()
                                    )
                                    db.collection("usuarios").document(uid).set(datos)
                                        .addOnSuccessListener {
                                            cargando = false
                                            onRegistroExitoso(recordar)
                                        }
                                        .addOnFailureListener { e ->
                                            cargando = false
                                            alcance.launch { snackbarHostState.showSnackbar(e.localizedMessage ?: "Error guardando perfil") }
                                        }
                                } else {
                                    cargando = false
                                    alcance.launch { snackbarHostState.showSnackbar("Error creando usuario") }
                                }
                            } else {
                                cargando = false
                                alcance.launch { snackbarHostState.showSnackbar(task.exception?.localizedMessage ?: "Error de registro") }
                            }
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !cargando
            ) {
                if (cargando) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Crear cuenta")
            }
        }
    }
}

@Composable
fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Buscar objetos o habilidades...") },
        leadingIcon = { Icon(Icons.Default.Search, "Buscar") },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(Icons.Default.Clear, "Limpiar")
                }
            }
        },
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

@Composable
fun CategoryChip(
    name: String,
    icon: ImageVector,
    seleccionada: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = seleccionada,
        onClick = onClick,
        label = { Text(name) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    )
}

@Composable
fun CategoriesSection(
    categoriaSeleccionada: String?,
    onCategoriaSeleccionadaChange: (String?) -> Unit
) {
    val categories = listOf(
        "Electrónica" to Icons.Default.Phone,
        "Libros" to Icons.Default.Menu,
        "Deportes" to Icons.Default.Favorite,
        "Música" to Icons.Default.Star,
        "Idiomas" to Icons.Default.Call,
        "Otro" to Icons.Default.MoreHoriz
    )

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Categorías destacadas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categories) { (name, icon) ->
                CategoryChip(
                    name = name,
                    icon = icon,
                    seleccionada = categoriaSeleccionada == name,
                    onClick = {
                        onCategoriaSeleccionadaChange(
                            if (categoriaSeleccionada == name) null else name
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun TruequeCard(
    item: TruequeItem,
    onClick: () -> Unit,
    onTipoClick: () -> Unit,
    onEliminar: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Imagen placeholder
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (item.tipo == TipoTrueque.OBJETO)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.secondaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (item.tipo == TipoTrueque.OBJETO)
                        Icons.Default.ShoppingCart
                    else
                        Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = if (item.tipo == TipoTrueque.OBJETO)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            // Información del item
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        item.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (onEliminar != null) {
                            IconButton(onClick = onEliminar) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                            }
                        }
                        AssistChip(
                            onClick = onTipoClick,
                            label = {
                                Text(
                                    if (item.tipo == TipoTrueque.OBJETO) "Objeto" else "Habilidad",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (item.tipo == TipoTrueque.OBJETO)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }

                Text(
                    item.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            item.usuario,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        item.categoria,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}