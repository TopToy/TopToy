
from __future__ import division
import glob
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
from matplotlib.axes import Axes

from utiles import csvs2df
fs=18

line_w=1
marker_s=5
face_c='none'
markers=['s', 'x', '+', '^']

def getYrange(index):
    if index == 1:
        return np.arange(0, 2501, 500)
    if index == 2:
        return np.arange(0, 12001, 3000)
    if index == 3:
        return np.arange(0, 15001, 3000)

def getY2range(index):
    if index == 1:
        return np.arange(0, 76, 25)
    if index == 2:
        return np.arange(0, 31, 10)
    if index == 3:
        return np.arange(0, 5, 1)


def tps(dirs, oPath):
    rows = 1
    cols = 3
    index = 1
    lab=['$n=4$', '$n=7$', '$n=10$']
    lab2 = [' ', ' ', '   ']
    nu="$n=$"
    # beta="$\\beta=$"
    beta = "$\\beta=$"
    names = [beta + '10'
        ,
             beta + '100'
        ,
             beta + '1000']
    n = 0
    txSize = [512]
    blockSize = [10, 100, 1000]
    fig, ax = plt.subplots(nrows=rows, ncols=cols, figsize=(8, 2.5))
    plt.subplots_adjust(wspace=0.2, hspace=0.5)
    r, c = 0, 0
    lines1 = []
    lines2 = []
    n_groups = 6
    index2 = np.arange(n_groups)
    markers_on = [0, 1, 3, 5]
    N = 6
    width = 0.35
    ind = [0.8, 2.9, 5]
    faulty_nodes = [1, 2, 3]
    for bs in blockSize:
        sb = str(rows) + str(cols) + str(index)
        sb = int(sb)
        ax1 = plt.subplot(sb)
        allData = []
        m = 0
        fni = 0
        for d in dirs:
            files = glob.glob(d + "/summeries/*.csv")
            df = csvs2df(files)
            mark = markers[m]
            fn = faulty_nodes[fni]
            fni+=1
            for ts in txSize:
                data = df[(df.txSize == ts) & (df.txInBlock == bs) & (df.workers <= 5) & (df.id >= fn)]
                data = data[['workers', 'duration', 'tps', 'syncs']]

                data = data[['workers', 'duration', 'tps', 'syncs']].groupby(df.workers).mean()

                res, = plt.plot(data['workers'], data['tps'], "-" + mark, markerfacecolor=face_c,
                             markersize=marker_s, linewidth=line_w, label=lab[m]) #, markevery=markers_on)
                lines1 += [res]
                allData += [data['syncs'] / data['duration']]
                m += 1
                # if minSyncs == -1:
                #     ax2.bar(data['workers'], data['syncs'] , 0.8,
                #         alpha=0.8, color='gray', #hatch='xx',
                #         label='forks')
                #     minSyncs = 0 #data['syncs']
                # else:
                #     # minSyncs = min(minSyncs, data['syncs'])
                #     ax2.bar(data['workers'], data['syncs'] - minSyncs, 0.8,
                #             alpha=0.8, color='gray',  # hatch='xx',
                #             label='forks')
        ax2 = ax1.twinx()
        lines1 += [ax2.bar(ind, allData[0] -allData[1], 0.6,
                alpha=0.3,  hatch='//', color='blue', bottom=allData[1], label=lab2[0])[0]]
        lines1 += [ax2.bar(ind, allData[1] - allData[2], 0.6,
                alpha=0.3,  hatch='...', color='orange', bottom=allData[2], label=lab2[1])[0]]
        lines1 += [ax2.bar(ind, allData[2], 0.6,
                alpha=0.3, hatch='xx', color='green', label=lab2[2])[0]]

        ax2.set_yticks(getY2range(index))
        ax2.tick_params(labelsize=fs)
        plt.title(names[n], fontsize=fs)

        plt.xticks(np.arange(0, 6, step=1), fontsize=fs)
        ax1.set_yticks(getYrange(index))
        ax1.tick_params(labelsize=fs)
        plt.grid(True)
        n += 1
        index += 1
    # handles, labels = ax1.get_legend_handles_labels()
    # ax1.legend(handles, labels,
    #            loc="lower center",  # Position of legend
    #                             # borderaxespad=0.01,  # Small spacing around legend box
    #                             fontsize=fs,
    #                             ncol=3,
    #                             frameon=False,
    #                             bbox_to_anchor=(2, -0.07),
    #            )


    handles2, labels2 = ax2.get_legend_handles_labels()
    # ax2.legend(handles2, labels2, loc="lower center",  # Position of legend
    #            # borderaxespad=0.01,  # Small spacing around legend box
    #            fontsize=fs,
    #            ncol=3,
    #            frameon=False,
    #            bbox_to_anchor=(0, -0.07))
    leg = fig.legend(lines1,  # The line objects
                     labels=lab,
                     # The labels for each line
                     loc="lower center",  # Position of legend
                     # borderaxespad=0.01,  # Small spacing around legend box
                     fontsize=fs,
                     ncol=3,
                     columnspacing=3,
                     frameon=False,
                     bbox_to_anchor=(0.53, -0.1),
                     #  title = "Tx size\n(Bytes)"
                     )
    leg = fig.legend(handles2,  # The line objects
                     labels2,
                     # The labels for each line
                     loc="lower center",  # Position of legend
                     # borderaxespad=0.01,  # Small spacing around legend box
                     fontsize=fs,
                     ncol=3,
                     columnspacing=5.1,
                     frameon=False,
                     bbox_to_anchor=(0.42, -0.1),
                     #  title = "Tx size\n(Bytes)"
                     )
    # leg = fig.legend(lines2,  # The line objects
    #                  labels=['$n=4$', '$n=7$', '$n=10$'],
    #                  # The labels for each line
    #                  loc="lower center",  # Position of legend
    #                  # borderaxespad=0.01,  # Small spacing around legend box
    #                  fontsize=fs,
    #                  ncol=3,
    #                  frameon=False,
    #                  bbox_to_anchor=(0.5, -0.07),
    #                  #  title = "Tx size\n(Bytes)"
    #                  )



    # plt.setp(leg.get_title(), fontsize='xx-small')
    fig.text(0.51, 0.11, "$\\omega$", ha="center", va="center", fontsize=fs)
    fig.text(0.02, 0.52, "TPS ($\\frac{transactions}{sec}$)", ha="center", va="center", fontsize=fs, rotation=90)
    fig.text(0.975, 0.52, "RPS ($\\frac{recoveries}{sec}$)", ha="center", va="center", fontsize=fs, rotation=-90)
    fig.tight_layout(rect=[0.03, 0.09, 0.97, 1.03])
    for d in oPath:
        plt.savefig(d + '/tps_byz1.pdf')
        plt.savefig(d + '/tps_byz1')

if __name__ == "__main__":
    tps(["/home/yoni/toy/m5/byz7/4"
         ,"/home/yoni/toy/m5/byz7/7"
         ,"/home/yoni/toy/m5/byz7/10"
             ],
        ["/home/yoni/toy/figures", "/home/yoni/Dropbox/paper/figures"])