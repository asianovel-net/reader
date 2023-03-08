package net.asianovel.reader.ui.widget.keyboard

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.view.setPadding
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.asianovel.reader.R
import net.asianovel.reader.base.BaseDialogFragment
import net.asianovel.reader.base.adapter.ItemViewHolder
import net.asianovel.reader.base.adapter.RecyclerAdapter
import net.asianovel.reader.data.appDb
import net.asianovel.reader.data.entities.KeyboardAssist
import net.asianovel.reader.databinding.DialogMultipleEditTextBinding
import net.asianovel.reader.databinding.DialogRecyclerViewBinding
import net.asianovel.reader.databinding.Item1lineTextAndDelBinding
import net.asianovel.reader.lib.dialogs.alert
import net.asianovel.reader.lib.theme.primaryColor
import net.asianovel.reader.ui.widget.recycler.ItemTouchCallback
import net.asianovel.reader.ui.widget.recycler.VerticalDivider
import net.asianovel.reader.utils.applyTint
import net.asianovel.reader.utils.dpToPx
import net.asianovel.reader.utils.setLayout
import net.asianovel.reader.utils.viewbindingdelegate.viewBinding
import net.asianovel.reader.utils.visible
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

class KeyboardAssistsConfig : BaseDialogFragment(R.layout.dialog_recycler_view),
    Toolbar.OnMenuItemClickListener {

    private val binding by viewBinding(DialogRecyclerViewBinding::bind)
    private val adapter by lazy { KeyAdapter(requireContext()) }

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, 0.9f)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.setTitle(R.string.assists_key_config)
        initView()
        initMenu()
        initData()
    }

    private fun initView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.addItemDecoration(VerticalDivider(requireContext()))
        binding.recyclerView.adapter = adapter
        val itemTouchCallback = ItemTouchCallback(adapter)
        itemTouchCallback.isCanDrag = true
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.recyclerView)

    }

    private fun initMenu() {
        binding.toolBar.setOnMenuItemClickListener(this)
        binding.toolBar.inflateMenu(R.menu.keyboard_assists_config)
        binding.toolBar.menu.applyTint(requireContext())
    }

    private fun initData() {
        launch {
            appDb.keyboardAssistsDao.flowAll.collect {
                adapter.setItems(it)
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_add -> editKey(null)
        }
        return false
    }

    private fun editKey(keyboardAssist: KeyboardAssist?) {
        alert {
            setTitle("辅助按键")
            val alertBinding = DialogMultipleEditTextBinding.inflate(layoutInflater).apply {
                layout1.hint = "key"
                edit1.setText(keyboardAssist?.key)
                layout2.hint = "value"
                layout2.visible()
                edit2.setText(keyboardAssist?.value)
            }
            setCustomView(alertBinding.root)
            cancelButton()
            okButton {
                launch(IO) {
                    val newKeyboardAssist = KeyboardAssist(
                        key = alertBinding.edit1.text.toString(),
                        value = alertBinding.edit2.text.toString()
                    )
                    if (keyboardAssist == null) {
                        newKeyboardAssist.serialNo = appDb.keyboardAssistsDao.maxSerialNo + 1
                        appDb.keyboardAssistsDao.insert(newKeyboardAssist)
                    } else {
                        newKeyboardAssist.serialNo = keyboardAssist.serialNo
                        appDb.keyboardAssistsDao.delete(keyboardAssist)
                        appDb.keyboardAssistsDao.insert(newKeyboardAssist)
                    }
                }
            }
        }
    }

    private inner class KeyAdapter(context: Context) :
        RecyclerAdapter<KeyboardAssist, Item1lineTextAndDelBinding>(context),
        ItemTouchCallback.Callback {

        private var isMoved = false

        override fun getViewBinding(parent: ViewGroup): Item1lineTextAndDelBinding {
            return Item1lineTextAndDelBinding.inflate(inflater, parent, false).apply {
                root.setPadding(16.dpToPx())
                ivDelete.visible()
            }
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: Item1lineTextAndDelBinding,
            item: KeyboardAssist,
            payloads: MutableList<Any>
        ) {
            binding.textView.text = item.key
        }

        override fun registerListener(holder: ItemViewHolder, binding: Item1lineTextAndDelBinding) {
            binding.root.setOnClickListener {
                getItemByLayoutPosition(holder.layoutPosition)?.let { keyboardAssist ->
                    editKey(keyboardAssist)
                }
            }
            binding.ivDelete.setOnClickListener {
                getItemByLayoutPosition(holder.layoutPosition)?.let { keyboardAssist ->
                    launch(IO) {
                        appDb.keyboardAssistsDao.delete(keyboardAssist)
                    }
                }
            }
        }

        override fun swap(srcPosition: Int, targetPosition: Int): Boolean {
            swapItem(srcPosition, targetPosition)
            isMoved = true
            return true
        }

        override fun onClearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            if (isMoved) {
                for ((index, item) in getItems().withIndex()) {
                    item.serialNo = index + 1
                }
                launch(IO) {
                    appDb.keyboardAssistsDao.update(*getItems().toTypedArray())
                }
            }
            isMoved = false
        }
    }
}