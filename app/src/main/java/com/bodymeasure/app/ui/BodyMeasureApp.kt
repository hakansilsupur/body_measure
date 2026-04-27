package com.bodymeasure.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bodymeasure.app.R
import com.bodymeasure.app.ui.theme.BodyMeasureTheme
import kotlinx.coroutines.launch

private enum class Tab { Add, History }

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BodyMeasureApp() {
    BodyMeasureTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val vm: MeasurementViewModel = viewModel()
            var tab by rememberSaveable { mutableStateOf(Tab.Add) }
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            val history by vm.history.collectAsState()

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.app_name)) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                },
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = tab == Tab.Add,
                            onClick = { tab = Tab.Add },
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            label = { Text(stringResource(R.string.tab_add)) }
                        )
                        NavigationBarItem(
                            selected = tab == Tab.History,
                            onClick = { tab = Tab.History },
                            icon = { Icon(Icons.Default.History, contentDescription = null) },
                            label = { Text(stringResource(R.string.tab_history)) }
                        )
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { padding ->
                val showMessage: (String) -> Unit = { msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
                androidx.compose.foundation.layout.Box(Modifier.padding(padding)) {
                    when (tab) {
                        Tab.Add -> AddMeasurementScreen(
                            onSave = vm::save,
                            showMessage = showMessage
                        )
                        Tab.History -> HistoryScreen(
                            items = history,
                            onDelete = { id ->
                                vm.delete(id)
                                showMessage("Entry deleted")
                            }
                        )
                    }
                }
            }
        }
    }
}
