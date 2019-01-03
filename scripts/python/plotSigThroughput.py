from __future__ import division
import glob
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

from utiles import csvs2df

line_w=1
marker_s=4
face_c='none'
markers=['s', 'x', '+', '^']

def plotSigThroughput(dirs, oPath):
    subDirs = ['10', '100', '1000']
    txSize = ['0', '512', '1024', '4096']
    # names = ['10 Txs/block', '100 Txs/block', '1000 Txs/block']
    names = ['50 Byte\nTransaction', '512 Byte\nTransaction', '1024 Byte\nTransaction', '4096 Byte\nTransaction']
    rows = 2
    cols = 2
    index = 1
    lines = []
    fig, ax = plt.subplots(nrows=rows, ncols=cols)
    plt.subplots_adjust(wspace=0.3, hspace=0.5)
    for size in txSize:
        for d in dirs:
            m = 0
            for sd in subDirs:
                mark = markers[m]
                m+=1
                sb = str(rows) + str(cols) + str(index)
                sb = int(sb)
                plt.subplot(sb)
                files = glob.glob(d + "/*." + size + "." + sd + "/servers/res/sig_summery.csv")
                df = csvs2df(files)
                df = df[['workers', 'sigPerSec']].groupby(df.workers).mean()
                markers_on=[0, 1, 2, 3]
                plt.plot(df['workers'], df['sigPerSec'] / 1000, "-" + mark, markerfacecolor=face_c,
                         markersize=6, linewidth=line_w, markevery=markers_on)


            plt.title(names[index - 1], fontsize='small')
            plt.xticks(np.arange(1, 5, step=1), fontsize='x-small')
            plt.yticks(np.arange(0, 8, step=1), fontsize='x-small')
            plt.grid(True)
            index += 1


    leg = fig.legend(lines,  # The line objects
                     # labels=['50', '512', '1024', '4096'],  # The labels for each line
                     labels=['10', '100', '1000'],  # The labels for each line
                     loc="upper right",  # Position of legend
                     borderaxespad=0.01,  # Small spacing around legend box
                     fontsize='x-small',
                     # frameon=False,
                     bbox_to_anchor=(0.91, 0.08),
                     title="Txs/block",
                     ncol=3
                     )
    plt.setp(leg.get_title(), fontsize='x-small')
    fig.text(0.49, 0.05, "Workers", ha="center", va="center", fontsize='small')
    fig.text(0.02, 0.5, "Throughput (Ksignatures/sec)", ha="center", va="center", fontsize='small', rotation=90)
    fig.tight_layout(rect=[0.02, 0.05, 0.935, 1])
    for d in oPath:
        plt.savefig(d + '/sig_throughput.pdf')
        plt.savefig(d + '/sig_throughput')

if __name__ == "__main__":
    plotSigThroughput(["/home/yoni/toy/res_sig"], ["/home/yoni/toy/figures", "/home/yoni/Dropbox/paper/draws"])