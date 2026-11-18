package com.example.semester7.unit4

import android.app.Application
import android.content.Context
import android.os.Bundle
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ============================================================================
// 1. ROOM ENTITY (Table Definition for Inventory Items)
// ============================================================================
@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val category: String,
    val quantity: Int,
    val price: Double,
    val description: String
)

// ============================================================================
// 2. ROOM DAO (Data Access Object)
// ============================================================================
@Dao
interface InventoryDao {
    // Screen 1: Reactively fetch all items emitting updates when database changes
    @Query("SELECT * FROM inventory_items ORDER BY id ASC")
    fun getAllItems(): Flow<List<InventoryItem>>

    // Screen 2: Query specific item details reactively by its unique primary key ID
    @Query("SELECT * FROM inventory_items WHERE id = :itemId")
    fun getItemById(itemId: Int): Flow<InventoryItem?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<InventoryItem>)
}

// ============================================================================
// 3. ROOM DATABASE (Singleton Database Instance)
// ============================================================================
@Database(entities = [InventoryItem::class], version = 1, exportSchema = false)
abstract class InventoryDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao

    companion object {
        @Volatile
        private var INSTANCE: InventoryDatabase? = null

        fun getDatabase(context: Context): InventoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InventoryDatabase::class.java,
                    "inventory_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ============================================================================
// 4. VIEWMODEL (Business Logic & State Management)
// ============================================================================
class InventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = InventoryDatabase.getDatabase(application).inventoryDao()

    // Screen 1 StateFlow: Reactive list of inventory items
    val itemList: StateFlow<List<InventoryItem>> = dao.getAllItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Seed sample data if the table is currently empty
        viewModelScope.launch {
            val existing = dao.getAllItems().first()
            if (existing.isEmpty()) {
                dao.insertAll(
                    listOf(
                        InventoryItem(
                            name = "Laptop Pro 15",
                            category = "Electronics",
                            quantity = 15,
                            price = 1299.99,
                            description = "High-performance workstation laptop featuring 16GB RAM, 512GB NVMe SSD, and dedicated graphics."
                        ),
                        InventoryItem(
                            name = "Ergonomic Wireless Mouse",
                            category = "Accessories",
                            quantity = 40,
                            price = 49.99,
                            description = "2.4GHz wireless ergonomic mouse with adjustable DPI and silent click technology."
                        ),
                        InventoryItem(
                            name = "Mechanical RGB Keyboard",
                            category = "Accessories",
                            quantity = 25,
                            price = 89.50,
                            description = "Tactile mechanical keyboard with customizable RGB per-key backlighting and anti-ghosting."
                        ),
                        InventoryItem(
                            name = "4K Ultra HD Monitor",
                            category = "Displays",
                            quantity = 10,
                            price = 349.00,
                            description = "27-inch 4K IPS display panel with HDR10, 99% sRGB color gamut, and ultra-narrow bezels."
                        ),
                        InventoryItem(
                            name = "USB-C Docking Station",
                            category = "Accessories",
                            quantity = 50,
                            price = 79.99,
                            description = "10-in-1 multi-port USB-C hub with dual HDMI, Ethernet, USB 3.0 ports, and 100W Power Delivery."
                        )
                    )
                )
            }
        }
    }

    // Screen 2 Query: Get item details reactively by primary key ID
    fun getItemDetails(itemId: Int): Flow<InventoryItem?> {
        return dao.getItemById(itemId)
    }

    // Insert new item
    fun addItem(name: String, category: String, quantity: Int, price: Double, description: String) {
        viewModelScope.launch {
            dao.insertItem(
                InventoryItem(
                    name = name,
                    category = category,
                    quantity = quantity,
                    price = price,
                    description = description
                )
            )
        }
    }
}

// ============================================================================
// 5. MAIN ACTIVITY & ENTRY POINT
// ============================================================================
class CA3 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    InventoryApp()
                }
            }
        }
    }
}

// ============================================================================
// 6. TWO-SCREEN NAVIGATION FLOW (NavHost)
// ============================================================================
@Composable
fun InventoryApp(viewModel: InventoryViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "inventory_list"
    ) {
        // SCREEN 1: Displays list of items fetched reactively from Room DB table
        composable("inventory_list") {
            InventoryListScreen(
                viewModel = viewModel,
                onItemClick = { itemId ->
                    // Navigate to screen two passing the item's primary key ID as navigation argument
                    navController.navigate("item_detail/$itemId")
                }
            )
        }

        // SCREEN 2: Accepts unique primary key ID as navigation argument & displays item details
        composable(
            route = "item_detail/{itemId}",
            arguments = listOf(
                navArgument("itemId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getInt("itemId") ?: 0
            ItemDetailScreen(
                itemId = itemId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

// ============================================================================
// 7. SCREEN ONE: INVENTORY ITEM LIST SCREEN
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryListScreen(
    viewModel: InventoryViewModel,
    onItemClick: (Int) -> Unit
) {
    // Reactive collection of Room database items
    val items by viewModel.itemList.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Inventory Items", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Tap any item to view its details (Passes ID via Navigation):",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        InventoryItemRow(
                            item = item,
                            onClick = { onItemClick(item.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddItemDialog(
            onDismiss = { showAddDialog = false },
            onAddItem = { name, category, quantity, price, desc ->
                viewModel.addItem(name, category, quantity, price, desc)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun InventoryItemRow(
    item: InventoryItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ID: #${item.id}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Category: ${item.category} | Qty: ${item.quantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "$${String.format(Locale.US, "%.2f", item.price)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

// ============================================================================
// 8. SCREEN TWO: ITEM DETAIL SCREEN (Queries Room DB by ID Navigation Argument)
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    itemId: Int,
    viewModel: InventoryViewModel,
    onBackClick: () -> Unit
) {
    // Query item details reactively from Room database using the passed primary key ID
    val itemState by remember(itemId) {
        viewModel.getItemDetails(itemId)
    }.collectAsStateWithLifecycle(initialValue = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Item Details (ID: #$itemId)") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            val item = itemState
            if (item == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Fetching item details from Room DB...")
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "ID: ${item.id}",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        DetailRow(label = "Category", value = item.category)
                        DetailRow(label = "Available Stock", value = "${item.quantity} units")
                        DetailRow(label = "Unit Price", value = "$${String.format(Locale.US, "%.2f", item.price)}")

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Description:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onBackClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Back to Inventory List")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ============================================================================
// 9. DIALOG FOR ADDING NEW INVENTORY ITEM
// ============================================================================
@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onAddItem: (String, String, Int, Double, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Inventory Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price ($)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantity.toIntOrNull() ?: 0
                    val prc = price.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        onAddItem(name, category, qty, prc, description)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
