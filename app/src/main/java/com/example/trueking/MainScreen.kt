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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipal() {
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

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
                    IconButton(onClick = { /* Notificaciones */ }) {
                        Badge {
                            Icon(Icons.Default.Notifications, "Notificaciones")
                        }
                    }
                    IconButton(onClick = { /* Perfil */ }) {
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
                onClick = { /* Crear nuevo trueque */ },
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
            CategoriesSection()

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

                items(itemsToShow) { item ->
                    TruequeCard(item)
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
fun CategoriesSection() {
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
                CategoryChip(name, icon)
            }
        }
    }
}

@Composable
fun CategoryChip(name: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.clickable { /* Filtrar por categoría */ }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun TruequeCard(item: TruequeItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Ver detalles */ },
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
                        onClick = { },
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