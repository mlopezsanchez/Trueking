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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.UUID

// Modelo de datos para los items de trueque
data class TruequeItem(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val tipo: TipoTrueque,
    val usuario: String,
    val valoracion: Float,
    val imagenUrl: String? = null
)

enum class TipoTrueque {
    OBJETO, HABILIDAD
}

private enum class RutaPantalla {
    PRINCIPAL,
    PERFIL,
    AGREGAR_TRUEQUE
}

@Composable
fun AppTrueque() {
    var rutaActual by rememberSaveable { mutableStateOf(RutaPantalla.PRINCIPAL) }
    var rutaVolverAgregar by rememberSaveable { mutableStateOf(RutaPantalla.PRINCIPAL) }
    val misTrueques = remember { mutableStateListOf<TruequeItem>() }

    when (rutaActual) {
        RutaPantalla.PRINCIPAL -> PantallaPrincipal(
            onPerfilClick = { rutaActual = RutaPantalla.PERFIL },
            onNuevoTruequeClick = {
                rutaVolverAgregar = RutaPantalla.PRINCIPAL
                rutaActual = RutaPantalla.AGREGAR_TRUEQUE
            }
        )

        RutaPantalla.PERFIL -> PantallaPerfil(
            onVolver = { rutaActual = RutaPantalla.PRINCIPAL },
            misTrueques = misTrueques,
            onAgregarTrueque = {
                rutaVolverAgregar = RutaPantalla.PERFIL
                rutaActual = RutaPantalla.AGREGAR_TRUEQUE
            }
        )

        RutaPantalla.AGREGAR_TRUEQUE -> PantallaAgregarTrueque(
            onVolver = { rutaActual = rutaVolverAgregar },
            onGuardar = { titulo, descripcion, tipo ->
                misTrueques.add(
                    TruequeItem(
                        id = UUID.randomUUID().toString(),
                        titulo = titulo,
                        descripcion = descripcion,
                        tipo = tipo,
                        usuario = "Ana García",
                        valoracion = 0f
                    )
                )
                rutaActual = RutaPantalla.PERFIL
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipal(
    onPerfilClick: () -> Unit,
    onNuevoTruequeClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var categoriaSeleccionada by rememberSaveable { mutableStateOf<String?>(null) }
    var itemDetalle by remember { mutableStateOf<TruequeItem?>(null) }
    val favoritos = remember { mutableStateListOf<String>() }
    val snackbarHostState = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()
    var notificacionesPendientes by rememberSaveable { mutableStateOf(3) }

    // Datos de ejemplo
    val itemsObjetos = listOf(
        TruequeItem("1", "Bicicleta de montaña", "Bicicleta en buen estado, ideal para rutas", TipoTrueque.OBJETO, "Ana García", 4.5f),
        TruequeItem("2", "Libro de programación", "Clean Code - Robert Martin", TipoTrueque.OBJETO, "Carlos López", 4.8f),
        TruequeItem("3", "Cafetera espresso", "Cafetera italiana, como nueva", TipoTrueque.OBJETO, "María Torres", 4.2f)
    )

    val itemsHabilidades = listOf(
        TruequeItem("4", "Clases de inglés", "Nivel B2-C1, 10 años de experiencia", TipoTrueque.HABILIDAD, "John Smith", 4.9f),
        TruequeItem("5", "Reparación de ordenadores", "Solución de problemas de hardware y software", TipoTrueque.HABILIDAD, "Pedro Ramírez", 4.6f),
        TruequeItem("6", "Clases de guitarra", "Todos los niveles, estilo rock y pop", TipoTrueque.HABILIDAD, "Laura Martín", 4.7f)
    )

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
                    IconButton(
                        onClick = {
                            alcance.launch {
                                if (notificacionesPendientes <= 0) {
                                    snackbarHostState.showSnackbar("No tienes notificaciones nuevas.")
                                } else {
                                    snackbarHostState.showSnackbar(
                                        "Tienes $notificacionesPendientes notificaciones pendientes (demo)."
                                    )
                                    notificacionesPendientes = 0
                                }
                            }
                        }
                    ) {
                        BadgedBox(
                            badge = {
                                if (notificacionesPendientes > 0) {
                                    Badge {
                                        Text(notificacionesPendientes.toString())
                                    }
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
                    1 -> itemsObjetos
                    2 -> itemsHabilidades
                    else -> itemsObjetos + itemsHabilidades
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
                            "Electrónica" -> item.titulo.contains("ordenador", ignoreCase = true) ||
                                item.descripcion.contains("hardware", ignoreCase = true) ||
                                item.descripcion.contains("software", ignoreCase = true)
                            "Libros" -> item.titulo.contains("libro", ignoreCase = true) ||
                                item.descripcion.contains("Clean Code", ignoreCase = true)
                            "Deportes" -> item.titulo.contains("bicicleta", ignoreCase = true)
                            "Música" -> item.titulo.contains("guitarra", ignoreCase = true) ||
                                item.descripcion.contains("rock", ignoreCase = true) ||
                                item.descripcion.contains("pop", ignoreCase = true)
                            "Idiomas" -> item.titulo.contains("inglés", ignoreCase = true) ||
                                item.descripcion.contains("inglés", ignoreCase = true)
                            else -> true
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
                    Text(
                        text = "Valoración: ${item.valoracion}",
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
                            alcance.launch {
                                snackbarHostState.showSnackbar(
                                    "Solicitud enviada a ${item.usuario} (demo)."
                                )
                            }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAgregarTrueque(
    onVolver: () -> Unit,
    onGuardar: (String, String, TipoTrueque) -> Unit
) {
    var titulo by rememberSaveable { mutableStateOf("") }
    var descripcion by rememberSaveable { mutableStateOf("") }
    var tipo by rememberSaveable { mutableStateOf(TipoTrueque.OBJETO) }
    var mostrarError by rememberSaveable { mutableStateOf(false) }

    val formularioValido = titulo.isNotBlank() && descripcion.isNotBlank()

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

            if (mostrarError) {
                Text(
                    text = "Completa el título y la descripción.",
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
                    onGuardar(titulo.trim(), descripcion.trim(), tipo)
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
    onAgregarTrueque: () -> Unit
) {
    val nombreUsuario = "Ana García"
    val usuario = "@ana.garcia"
    val ubicacion = "Madrid"
    val valoracionMedia = 4.7f
    val truequesRealizados = 18
    val truequesActivos = misTrueques.size
    val truequesFavoritos = 12

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Perfil", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        }
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

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color(0xFFFFB300)
                                        )
                                        Text(
                                            text = valoracionMedia.toString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            OutlinedButton(onClick = { }) {
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
                        onTipoClick = { }
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ListItem(
                            headlineContent = { Text("Mis trueques") },
                            supportingContent = { Text("Gestiona tus publicaciones activas") },
                            leadingContent = { Icon(Icons.Default.List, contentDescription = null) },
                            trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                            modifier = Modifier.clickable { }
                        )
                        Divider()
                        ListItem(
                            headlineContent = { Text("Historial") },
                            supportingContent = { Text("Trueques finalizados y valoraciones") },
                            leadingContent = { Icon(Icons.Default.History, contentDescription = null) },
                            trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                            modifier = Modifier.clickable { }
                        )
                        Divider()
                        ListItem(
                            headlineContent = { Text("Direcciones") },
                            supportingContent = { Text("Entrega y puntos de encuentro") },
                            leadingContent = { Icon(Icons.Default.Place, contentDescription = null) },
                            trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                            modifier = Modifier.clickable { }
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ListItem(
                            headlineContent = { Text("Privacidad") },
                            supportingContent = { Text("Controla tu visibilidad") },
                            leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                            modifier = Modifier.clickable { }
                        )
                        Divider()
                        ListItem(
                            headlineContent = { Text("Notificaciones") },
                            supportingContent = { Text("Configura avisos y alertas") },
                            leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) },
                            trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                            modifier = Modifier.clickable { }
                        )
                        Divider()
                        ListItem(
                            headlineContent = { Text("Ayuda") },
                            supportingContent = { Text("Centro de ayuda y soporte") },
                            leadingContent = { Icon(Icons.Default.Help, contentDescription = null) },
                            trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                            modifier = Modifier.clickable { }
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = { },
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
fun CategoriesSection(
    categoriaSeleccionada: String?,
    onCategoriaSeleccionadaChange: (String?) -> Unit
) {
    val categories = listOf(
        "Electrónica" to Icons.Default.Phone,
        "Libros" to Icons.Default.Menu,
        "Deportes" to Icons.Default.Favorite,
        "Música" to Icons.Default.Star,
        "Idiomas" to Icons.Default.Call
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
fun CategoryChip(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    seleccionada: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = seleccionada,
        onClick = onClick,
        label = { Text(name) },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    )
}

@Composable
fun TruequeCard(
    item: TruequeItem,
    onClick: () -> Unit,
    onTipoClick: () -> Unit
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

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFFFB300)
                        )
                        Text(
                            item.valoracion.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}