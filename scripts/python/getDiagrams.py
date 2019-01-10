from __future__ import division
import glob
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

line_w=1
marker_s=4
face_c='none'
markers=['s', 'x', '+', '^']

    # if index == 7:
    #     return np.arange(0, 11, 2)
    # if index == 8:
    #     return np.arange(0, 81, 20)
    # if index == 9:
    #     return np.arange(0, 301, 100)


def drawSigs(dirs, oPath):
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
                path = d + "/" + sd + "/sig_summery_" + size + ".csv"
                df = pd.read_csv(path, sep=",")
                df = df[['workers', 'sigPerSec']]
                markers_on=[0, 1, 2, 3]
                plt.plot(df['workers'], df['sigPerSec'] / 1000, "-" + mark, markerfacecolor=face_c,
                         markersize=6, linewidth=line_w, markevery=markers_on)


            plt.title(names[index - 1], fontsize='small')
            # plt.xlabel('workers', fontsize='small')
            # plt.ylabel('Throughput (Ksignatures/sec)', fontsize='small')
            plt.xticks(np.arange(1, 5, step=1), fontsize='x-small')
            plt.yticks(np.arange(0, 8, step=1), fontsize='x-small')
            plt.grid(True)
            index += 1


    leg = fig.legend(lines,  # The line objects
                     # labels=['50', '512', '1024', '4096'],  # The labels for each line
                     labels=['10', '100', '1000'],  # The labels for each line
                     loc="upper right",  # Position of legend
                     borderaxespad=0.01,  # Small spacing around legend box
                     fontsize='xx-small',
                     # frameon=False,
                     bbox_to_anchor=(0.992, 0.90),
                     title="Txs/block"
                     )
    plt.setp(leg.get_title(), fontsize='xx-small')
    fig.text(0.49, 0.02, "Workers", ha="center", va="center", fontsize='small')
    fig.text(0.02, 0.5, "Throughput (Ksignatures/sec)", ha="center", va="center", fontsize='small', rotation=90)
    fig.tight_layout(rect=[0.02, 0, 0.935, 1])
    for d in oPath:
        plt.savefig(d + '/sig_throughput')

def calcCDFX(index):
    if index == 1:
        return np.arange(0, 2501, 500)
    if index == 2:
        return np.arange(0, 7001, 1000)
    if index == 3:
        return np.arange(0, 3001, 500)
    if index == 4:
        return np.arange(0, 9001, 1000)
    if index == 5:
        return np.arange(0, 6001, 1000)
    if index == 6:
        return np.arange(0, 11001, 1000)
def drawCDF(dirs, oPath):
    subDirs = ["clients/500.100", "clients/500.1000"]
    names = ['4 Servers 100 Txs/Block', '4 Servers 1000 Txs/Block', '7 Servers 100 Txs/Block',
             '7 Servers 1000 Txs/Block', '10 Servers 100 Txs/Block', '10 Servers 1000 Txs/Block']
    rows = 3
    cols = 2
    index = 1
    n=0
    fig, ax = plt.subplots(nrows=rows, ncols=cols)
    plt.subplots_adjust(wspace=0.3, hspace=0.5)
    lines = []
    for dir in dirs:
        for d in subDirs:
            files = ["blocksStat_1.csv", "blocksStat_5.csv", "blocksStat_10.csv"]
            m=0
            for f in files:
                mark = markers[m]
                m+=1
                sb = str(rows) + str(cols) + str(index)
                sb = int(sb)
                plt.subplot(sb)
                path = dir + "/" + d + "/" + f
                df = pd.read_csv(path, sep=",")
                df = df['clientLatency']
                num_bins = 100
                counts, bin_edges = np.histogram(df /1000, bins=num_bins, normed=True)
                cdf = np.cumsum(counts)
                markers_on=[0, 33, 66, 99]
                plt.plot(bin_edges[1:], cdf / cdf[-1], "-" + mark, markerfacecolor=face_c,
                         markersize=6, linewidth=line_w, markevery=markers_on)
                # cdf = np.cumsum(df)
                # x = np.arange(0, 5000, 500)
                # l = plt.plot(df, cdf)

            plt.title(names[n], fontsize='x-small')
            plt.xticks(calcCDFX(index) / 1000, fontsize='xx-small')
            plt.yticks(np.arange(0, 1.01, 0.2), fontsize='xx-small')
            plt.grid(True)
            n += 1
            index += 1

    leg = fig.legend(lines,  # The line objects
                     labels=['1', '5', '10'],  # The labels for each line
                     loc="upper right",  # Position of legend
                     borderaxespad=0.01,  # Small spacing around legend box
                     fontsize='xx-small',
                     # frameon=False,
                     bbox_to_anchor=(0.99, 0.935),
                     title="Channels"
                     )
    plt.setp(leg.get_title(), fontsize='xx-small')
    # fig.text(0.5, 0.04, "Latency (seconds)", ha="center", va="center")
    # fig.text(0.05, 0.5, "percents of requests", ha="center", va="center", rotation=90)

    fig.text(0.5, 0.02, "Time (seconds)", ha="center", fontsize='small', va="center")
    fig.text(0.02, 0.5, "Probability", ha="center", va="center", fontsize='small', rotation=90)
    fig.tight_layout(rect=[0.02, 0, 0.945, 1])
    for d in oPath:
        plt.savefig(d + '/cdf')

def drawGDCDF2(dirs, oPath):
    n_groups = 3
    bar_width = 0.35
    opacity = 1
    subDirs = ["clients/500.100", "clients/500.1000"]
    names = ['100 Txs/block', '1000 Txs/block']
    rows = 1
    cols = 2
    index = 1
    n=0
    fig, ax = plt.subplots(nrows=rows, ncols=cols)
    plt.subplots_adjust(wspace=0.3, hspace=0.5)
    lines = []
    for d in subDirs:
        sb = str(rows) + str(cols) + str(index)
        sb = int(sb)
        ax2 = plt.subplot(sb)
        index2 = np.arange(n_groups)
        bars = [[], []]
        bars2 = [[], []]
        for dir in dirs:

            files = ["blocksStat_1.csv", "blocksStat_5.csv"]
            for i in range(0, 2):
                f = files[i]
                path = dir + "/" + d + "/" + f
                df = pd.read_csv(path, sep=",")
                df = df['clientLatency']
                med = df.median()
                bars[i] += [med / 1000]
                eith = df.quantile(0.8) - med
                bars2[i] += [eith / 1000]
                # cdf = np.cumsum(df)
                # x = np.arange(0, 5000, 500)
                # l = plt.plot(df, cdf)

        plt.bar(index2, bars[0], bar_width,
                alpha=opacity, hatch='xxxx')
        plt.bar(index2, bars2[0], bar_width,
               bottom=bars[0], alpha=opacity, hatch='....')
        plt.bar(index2 + bar_width, bars[1], bar_width,
                alpha=opacity)
        plt.bar(index2 + bar_width, bars2[1], bar_width,
                bottom=bars[1], alpha=opacity, hatch='////')

        ax2.set_xticks(np.arange(n_groups) + bar_width / 2)
        ax2.set_xticklabels(('4', '7', '10'))

        # plt.title(names[n], fontsize='x-small')
        # plt.xticks(calcCDFX(index) / 1000, fontsize='xx-small')
        # plt.yticks(np.arange(0, 1.01, 0.2), fontsize='xx-small')
        plt.title(names[index - 1], fontsize='small')
        plt.grid(True)
        index += 1

    leg = fig.legend([],  # The line objects
                     labels=['1(0.5)', '1(0.8)', '5(0.5)', '5(0.8)'],  # The labels for each line
                     loc="upper right",  # Position of legend
                     borderaxespad=0.01,  # Small spacing around legend box
                     fontsize='x-small',
                     # frameon=False,
                     handlelength=0.9,
                     bbox_to_anchor=(0.985, 0.925),
                     title="Channels\n (centile)",
                     # handletextpad=0.5,
                     # title_fontsize="xx-small"
                     )
    plt.setp(leg.get_title(), fontsize='xx-small')
    # plt.setp(leg.get_title(), fontsize='xx-small')
    # fig.text(0.5, 0.04, "Latency (seconds)", ha="center", va="center")
    # fig.text(0.05, 0.5, "percents of requests", ha="center", va="center", rotation=90)

    fig.text(0.48, 0.04, "Servers", ha="center", va="center")
    fig.text(0.03, 0.5, "Time (seconds)", ha="center", va="center", rotation=90)
    fig.tight_layout(rect=[0.02, 0, 0.93, 1])
    # fig.tight_layout(rect=[0.04, 0, 0.9, 1])
    for d in oPath:
        plt.savefig(d + '/gd_cdf', bbox_inches='tight')






def drawGDCDF2(dirs, oPath):
    n_groups = 3
    bar_width = 0.35
    opacity = 1
    subDirs = ["clients/500.100", "clients/500.1000"]
    names = ['100 Txs/block', '1000 Txs/block']
    rows = 1
    cols = 2
    index = 1
    n=0
    fig, ax = plt.subplots(nrows=rows, ncols=cols)
    plt.subplots_adjust(wspace=0.3, hspace=0.5)
    lines = []
    for d in subDirs:
        sb = str(rows) + str(cols) + str(index)
        sb = int(sb)
        ax2 = plt.subplot(sb)
        index2 = np.arange(n_groups)
        bars = [[], []]
        bars2 = [[], []]
        for dir in dirs:

            files = ["blocksStat_1.csv", "blocksStat_5.csv"]
            for i in range(0, 2):
                f = files[i]
                path = dir + "/" + d + "/" + f
                df = pd.read_csv(path, sep=",")
                df = df['clientLatency']
                med = df.median()
                bars[i] += [med / 1000]
                eith = df.quantile(0.8) - med
                bars2[i] += [eith / 1000]
                # cdf = np.cumsum(df)
                # x = np.arange(0, 5000, 500)
                # l = plt.plot(df, cdf)

        plt.bar(index2, bars[0], bar_width,
                alpha=opacity, hatch='xxxx')
        plt.bar(index2, bars2[0], bar_width,
               bottom=bars[0], alpha=opacity, hatch='....')
        plt.bar(index2 + bar_width, bars[1], bar_width,
                alpha=opacity)
        plt.bar(index2 + bar_width, bars2[1], bar_width,
                bottom=bars[1], alpha=opacity, hatch='////')

        ax2.set_xticks(np.arange(n_groups) + bar_width / 2)
        ax2.set_xticklabels(('4', '7', '10'))

        # plt.title(names[n], fontsize='x-small')
        # plt.xticks(calcCDFX(index) / 1000, fontsize='xx-small')
        # plt.yticks(np.arange(0, 1.01, 0.2), fontsize='xx-small')
        plt.title(names[index - 1], fontsize='small')
        plt.grid(True)
        index += 1

    leg = fig.legend([],  # The line objects
                     labels=['1(0.5)', '1(0.8)', '5(0.5)', '5(0.8)'],  # The labels for each line
                     loc="upper right",  # Position of legend
                     borderaxespad=0.01,  # Small spacing around legend box
                     fontsize='x-small',
                     # frameon=False,
                     handlelength=0.9,
                     bbox_to_anchor=(0.985, 0.925),
                     title="Channels\n (centile)",
                     # handletextpad=0.5,
                     # title_fontsize="xx-small"
                     )
    plt.setp(leg.get_title(), fontsize='xx-small')
    # plt.setp(leg.get_title(), fontsize='xx-small')
    # fig.text(0.5, 0.04, "Latency (seconds)", ha="center", va="center")
    # fig.text(0.05, 0.5, "percents of requests", ha="center", va="center", rotation=90)

    fig.text(0.48, 0.04, "Servers", ha="center", va="center")
    fig.text(0.03, 0.5, "Time (seconds)", ha="center", va="center", rotation=90)
    fig.tight_layout(rect=[0.02, 0, 0.93, 1])
    # fig.tight_layout(rect=[0.04, 0, 0.9, 1])
    for d in oPath:
        plt.savefig(d + '/gd_cdf', bbox_inches='tight')



def getGDYrange(index):
    if index == 1:
        return np.arange(0, 15, 3)
    if index == 2:
        return np.arange(0, 15, 3)




def drawBSThroghuputCharts(dir, oPath):
    # rows = 1
    # cols = 1
    # index = 1
    # fig, ax = plt.subplots(nrows=rows, ncols=cols)
    # plt.subplots_adjust(wspace=0.2, hspace=0.5)
    # names = ['100 Txs/block', '1000 Txs/block']
    # subDirs = ['4Servers', '7Servers', '10Servers']
    evSize = ['100', '1000']
    m = 0
    fig = plt.figure(1)
    # sb = str(rows) + str(cols) + str(index)
    # sb = int(sb)
    # plt.subplot(sb, aspect='equal', adjustable='box-forced')
    for e in evSize:
        sem = glob.glob(dir + "*/*." + e + "/servers/res/summery.csv")[0]
        mark = markers[m]
        m += 1
        df = pd.read_csv(sem, sep=",")
        df = df[['channels', 'txPsec']].groupby(df.channels).mean()
        markers_on = [0, 5, 10, 19]
        plt.plot(df['channels'], df['txPsec'] / 1000, "-" + mark,
                 markerfacecolor=face_c, markersize=marker_s,
                 linewidth=line_w, markevery=markers_on)

    # plt.title(names[index - 1], fontsize='x-small')
    plt.xticks(np.arange(0, 21, step=5), fontsize='x-small')
    plt.yticks(np.arange(0, 46, step=5), fontsize='x-small')
    plt.grid(True)
    # index += 1
    # plt.figlegend(lines, ('label1', 'label2', 'label3'), 'upper right')
    leg = fig.legend([],  # The line objects
               labels=['100', '1000'],  # The labels for each line
               loc="upper right",  # Position of legend
               borderaxespad=0.01,  # Small spacing around legend box
               fontsize='xx-small',
               # frameon=False,
               bbox_to_anchor=(0.99, 0.98),
                title = "Txs/block"
               )
    plt.setp(leg.get_title(), fontsize='xx-small')
    fig.text(0.47, 0.01, "Channels", ha="center", va="center", fontsize='small')
    fig.text(0.02, 0.5, "Throughput (KTxs/sec)", ha="center", va="center", rotation=90, fontsize='small')
    fig.tight_layout(rect=[0.015, 0, 0.93, 1])
    for d in oPath:
        plt.savefig(d + '/BS_throughput', bbox_inches = 'tight', pad_inches = 0.08)






    #

    # drawGDCDF2(["/home/yoni/toy/gd_latency/4Servers"
    #               , "/home/yoni/toy/gd_latency/7Servers"
    #               , "/home/yoni/toy/gd_latency/10Servers"]
    #           , ["/home/yoni/toy/figures", "/home/yoni/Dropbox/paper/draws"])

    # heatmaps2(["/home/yoni/toy/gd_latency/4Servers"
    #             , "/home/yoni/toy/gd_latency/7Servers"
    #             , "/home/yoni/toy/gd_latency/10Servers"]
    #         , ["/home/yoni/toy/figures", "/home/yoni/Dropbox/paper/draws"])
    # drawBSThroghuputCharts('/home/yoni/toy/bsThroughput',
    #                        ["/home/yoni/toy/figures", "/home/yoni/Dropbox/paper/draws"])