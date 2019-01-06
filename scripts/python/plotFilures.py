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

def plotFailures(dir, sizes, oPath):
    rows = 1
    cols = 2
    index = 1
    n_groups = 3
    bar_width = 0.35
    opacity = 1
    names = ['100 Txs/block', '1000 Txs/block']
    fig, ax = plt.subplots(nrows=rows, ncols=cols)
    plt.subplots_adjust(wspace=0.2, hspace=0.5)
    for s in sizes:
        sb = str(rows) + str(cols) + str(index)
        sb = int(sb)
        ax2 = plt.subplot(sb) #, aspect='equal', adjustable='box-forced')
        benginFiles = glob.glob(dir + "/*." + s + ".bengin/servers/res/summery.csv")
        list_ = []
        for f in benginFiles:
            df = pd.read_csv(f, index_col=None, header=0)
            list_.append(df)
        benginDf = pd.concat(list_, axis=0, ignore_index=True)

        byzFiles = glob.glob(dir + "/*." + s + ".byz/servers/res/summery.csv")
        list_ = []
        for f in byzFiles:
            df = pd.read_csv(f, index_col=None, header=0)
            list_.append(df)
        byzDf = pd.concat(list_, axis=0, ignore_index=True)

        benginDf = benginDf[benginDf.id > 0]
        byzDf = byzDf[byzDf.id > 0]

        index2 = np.arange(n_groups)


        benginDf = benginDf[['channels', 'txPsec']].groupby(benginDf.channels).mean()
        byzDf = byzDf[['channels', 'txPsec']].groupby(byzDf.channels).mean()

        rects1 = plt.bar(index2, benginDf['txPsec'] / 1000, bar_width,
                         alpha=opacity, hatch='xxx',
                        label='Benign fault')
        rects2 = plt.bar(index2 + bar_width, byzDf['txPsec'] / 1000, bar_width,
                         alpha=opacity,
                         label='Byzantine fault')
        ax2.set_xticks(index2 + bar_width / 2)
        ax2.set_xticklabels(('1', '5', '10'))
        # plt.yticks(getYrange(index), fontsize='x-small')
        plt.title(names[index-1], fontsize='small')
        index += 1

    leg = fig.legend([],  # The line objects
                     labels=['Omission', 'Byzantine'],  # The labels for each line
                     loc="upper right",  # Position of legend
                     borderaxespad=0.01,  # Small spacing around legend box
                     fontsize='xx-small',
                     handlelength=0.9,
                     # frameon=False,
                     bbox_to_anchor=(0.995, 0.93),
                     title="Fault Type",
                     # mode="expand"
                     handletextpad=0.5
                     )
    plt.setp(leg.get_title(), fontsize='xx-small')
    # plt.set_xlabel('channels')
    # plt.set_ylabel('Throughput (KTxs / sec)')

    # ax.legend()
    #
    # fig.tight_layout()

    fig.text(0.5, 0.03, "Channels", ha="center", va="center")
    fig.text(0.03, 0.5, "Throughput (KTxs/sec)", ha="center", va="center", rotation=90)
    fig.tight_layout(rect=[0.02, 0, 0.925, 1])
    for d in oPath:
        plt.savefig(d + '/failures.pdf')
        plt.savefig(d + '/failures')

if __name__ == "__main__":
    plotFailures("/home/yoni/toy/res_fail", ['100', '1000'],
                 ["/home/yoni/toy/figures", "/home/yoni/Dropbox/paper/draws"])
