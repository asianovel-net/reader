package net.asianovel.reader.ui.config

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import net.asianovel.reader.R
import net.asianovel.reader.base.BaseDialogFragment
import net.asianovel.reader.base.adapter.ItemViewHolder
import net.asianovel.reader.base.adapter.RecyclerAdapter
import net.asianovel.reader.databinding.DialogRecyclerViewBinding
import net.asianovel.reader.databinding.ItemThemeConfigBinding
import net.asianovel.reader.help.config.ThemeConfig
import net.asianovel.reader.lib.dialogs.alert
import net.asianovel.reader.lib.theme.primaryColor
import net.asianovel.reader.ui.widget.recycler.VerticalDivider
import net.asianovel.reader.utils.*
import net.asianovel.reader.utils.viewbindingdelegate.viewBinding

class ThemeListDialog : BaseDialogFragment(R.layout.dialog_recycler_view),
    Toolbar.OnMenuItemClickListener {

    private val binding by viewBinding(DialogRecyclerViewBinding::bind)
    private val adapter by lazy { Adapter(requireContext()) }

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, 0.9f)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.setTitle(R.string.theme_list)
        initView()
        initMenu()
        initData()
    }

    private fun initView() = binding.run {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.addItemDecoration(VerticalDivider(requireContext()))
        recyclerView.adapter = adapter
    }

    private fun initMenu() = binding.run {
        toolBar.setOnMenuItemClickListener(this@ThemeListDialog)
        toolBar.inflateMenu(R.menu.theme_list)
        toolBar.menu.applyTint(requireContext())
    }

    fun initData() {
        adapter.setItems(ThemeConfig.configList)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_import -> {
                requireContext().getClipText()?.let {
                    if (ThemeConfig.addConfig(it)) {
                        initData()
                    } else {
                        toastOnUi("格式不对,添加失败")
                    }
                }
            }
        }
        return true
    }

    fun delete(index: Int) {
        alert(R.string.delete, R.string.sure_del) {
            yesButton {
                ThemeConfig.delConfig(index)
                initData()
            }
            noButton()
        }
    }

    fun share(index: Int) {
        val json = GSON.toJson(ThemeConfig.configList[index])
        requireContext().share(json, "主题分享")
    }

    inner class Adapter(context: Context) :
        RecyclerAdapter<ThemeConfig.Config, ItemThemeConfigBinding>(context) {

        override fun getViewBinding(parent: ViewGroup): ItemThemeConfigBinding {
            return ItemThemeConfigBinding.inflate(inflater, parent, false)
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemThemeConfigBinding,
            item: ThemeConfig.Config,
            payloads: MutableList<Any>
        ) {
            binding.apply {
                tvName.text = item.themeName
            }
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemThemeConfigBinding) {
            binding.apply {
                root.setOnClickListener {
                    ThemeConfig.applyConfig(context, ThemeConfig.configList[holder.layoutPosition])
                }
                ivShare.setOnClickListener {
                    share(holder.layoutPosition)
                }
                ivDelete.setOnClickListener {
                    delete(holder.layoutPosition)
                }
            }
        }

    }
}