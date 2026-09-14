package com.bodymeasure.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bodymeasure.app.R
import com.bodymeasure.app.data.MeasurementBackup
import com.bodymeasure.app.ui.theme.BodyMeasureTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Tab { Add, History, Trends }

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BodyMeasureApp() {
    BodyMeasureTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val vm: MeasurementViewModel = viewModel()
            var tab by rememberSaveable { mutableStateOf(Tab.Add) }
            val stateHolder = rememberSaveableStateHolder()
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            val history by vm.history.collectAsState()

            // Edit target lives in the ViewModel; resolve against the live list so an
            // external delete drops us back into "new entry" mode instead of stale state.
            val editingId = vm.editingId
            val isEditing = editingId != null && history.any { it.id == editingId }
            val draft = if (isEditing) vm.editDraft else vm.newDraft

            val showMessage: (String) -> Unit = { msg ->
                scope.launch { snackbarHostState.showSnackbar(msg) }
            }

            val exportEmptyMsg = stringResource(R.string.export_empty)
            val exportDoneFmt = stringResource(R.string.export_done)
            val importDoneFmt = stringResource(R.string.import_done)
            val importSkipsFmt = stringResource(R.string.import_done_with_skips)
            val importNoneNew = stringResource(R.string.import_none_new)

            val onTransferResult: (TransferResult) -> Unit = { result ->
                showMessage(
                    when (result) {
                        is TransferResult.Exported -> exportDoneFmt.format(result.count)
                        is TransferResult.Imported -> when {
                            result.added == 0 -> importNoneNew
                            result.skipped > 0 -> importSkipsFmt.format(result.added, result.skipped)
                            else -> importDoneFmt.format(result.added)
                        }
                        TransferResult.Empty -> exportEmptyMsg
                        is TransferResult.Failed -> result.message
                    }
                )
            }

            val exportLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument(MeasurementBackup.MIME_TYPE)
            ) { uri -> uri?.let { vm.exportTo(it, onTransferResult) } }

            val importLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri -> uri?.let { vm.importFrom(it, onTransferResult) } }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.app_name)) },
                        actions = {
                            DataMenu(
                                onExport = {
                                    exportLauncher.launch(
                                        MeasurementBackup.suggestedFileName(todayStamp())
                                    )
                                },
                                // Some file providers label .json as octet-stream, so
                                // accept both rather than hiding the user's own backup.
                                onImport = {
                                    importLauncher.launch(
                                        arrayOf(MeasurementBackup.MIME_TYPE, "application/octet-stream", "text/plain")
                                    )
                                }
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                        NavigationBarItem(
                            selected = tab == Tab.Trends,
                            onClick = { tab = Tab.Trends },
                            icon = { Icon(Icons.Default.ShowChart, contentDescription = null) },
                            label = { Text(stringResource(R.string.tab_trends)) }
                        )
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { padding ->
                val deletedLabel = stringResource(R.string.deleted)
                Box(Modifier.padding(padding)) {
                    // Each tab's scroll position and transient UI state is saved when
                    // it leaves the composition. Form field values live in the
                    // ViewModel draft, so they persist regardless.
                    stateHolder.SaveableStateProvider(tab.name) {
                        when (tab) {
                            Tab.Add -> AddMeasurementScreen(
                                draft = draft,
                                onSave = vm::save,
                                showMessage = showMessage,
                                isEditing = isEditing,
                                editingId = editingId,
                                onCancelEdit = { vm.stopEdit() },
                                onEditDone = {
                                    vm.stopEdit()
                                    tab = Tab.History
                                }
                            )
                            Tab.History -> HistoryScreen(
                                items = history,
                                onDelete = { id ->
                                    if (editingId == id) vm.stopEdit()
                                    vm.delete(id)
                                    showMessage(deletedLabel)
                                },
                                onEdit = { measurement ->
                                    vm.startEdit(measurement)
                                    tab = Tab.Add
                                }
                            )
                            Tab.Trends -> TrendsScreen(items = history)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DataMenu(onExport: () -> Unit, onImport: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.export_data)) },
            leadingIcon = { Icon(Icons.Default.Upload, contentDescription = null) },
            onClick = { expanded = false; onExport() }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.import_data)) },
            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
            onClick = { expanded = false; onImport() }
        )
    }
}

private fun todayStamp(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
