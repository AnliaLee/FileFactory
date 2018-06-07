package com.anlia.example;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import com.anlia.library.model.FileModel;
import com.anlia.library.utils.ScanDirectoryUtil;

import java.util.List;

/**
 * Created by anlia on 2018/4/23.
 */
public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.ViewHolder> {

    private Context context;
    private List<FileModel> list;
    private OnItemClickListener mOnItemClickListener;

    public FileListAdapter(Context context, List<FileModel> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // TODO: 为对应itemViewId赋值,例：R.layout.xxx
        int itemViewId = R.layout.item_file;
        ViewHolder holder = new ViewHolder(LayoutInflater.from(context).inflate(itemViewId, parent, false));
        return holder;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        // TODO: 声明组件
        ImageView imgFile;
        TextView textFile;

        public ViewHolder(View view) {
            super(view);
            // TODO: 注册组件,view.findViewById(R.id.xxx)
            imgFile = view.findViewById(R.id.img_file);
            textFile = view.findViewById(R.id.text_file);
        }

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        // TODO: 绑定组件的事件
        switch (list.get(position).getFileType()){
            case ScanDirectoryUtil.FILE_TYPE_SD:
                holder.imgFile.setImageResource(R.drawable.icon_sd);
                break;
            case ScanDirectoryUtil.FILE_TYPE_FOLDER:
                holder.imgFile.setImageResource(R.drawable.icon_directory);
                break;
            case ScanDirectoryUtil.FILE_TYPE_FILE:
                holder.imgFile.setImageResource(R.drawable.icon_file);
                break;
        }

        holder.textFile.setText(list.get(position).getFileName());

        // 如果设置了回调，则设置点击事件
        if (mOnItemClickListener != null) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = holder.getLayoutPosition();
                    mOnItemClickListener.onItemClick(holder.itemView, pos);
                }
            });

            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int pos = holder.getLayoutPosition();
                    mOnItemClickListener.onItemLongClick(holder.itemView, pos);
                    return true;
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void setOnItemClickListener(OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);

        void onItemLongClick(View view, int position);
    }
}