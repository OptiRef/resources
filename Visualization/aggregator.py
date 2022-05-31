import pandas as pd
import os
import matplotlib.pyplot as plt
import seaborn as sns
import tqdm

from datetime import date
import colorsys

psql ="(POSTGRES 14.2)"
mysql = "(MYSQL 8.0.25)"
db2 = "(DB2 11.5)"
rdbmsdic = {"mysql": mysql, "psql": psql, "db2":db2}

def average_df(df, cost=False):

    queries = list(df["query"])
    if cost:
        ex_time = list(df["COST"])
        total = list(df["COST"])# + df["TPRUN"]) #EXEC_TIME
    else:
        ex_time = list(df["TOTAL"])
        total = list(df["TOTAL"])# + df["TPRUN"]) #EXEC_TIME
    dic = {}
    for q in range(len(queries)):
        x = total[q]
        y = ex_time[q]
        if y<0: x = -1
        try:
            dic[queries[q]] += [x]
        except:
            dic[queries[q]] = [x]
    
    for q in dic:
        dic[q] = sum(dic[q])/len(dic[q])

    key_queries = dic.keys()
    total = dic.values()
    return pd.DataFrame({"query" : key_queries, "TOTAL" : total})

def get_NREF(df):
    
    nref = list(df["NREF"])
    queries = list(df["query"])
    dicnref = {}
    for q in range(len(queries)):
        dicnref[queries[q]] = nref[q]
    
    return list(dicnref.values())


def get_feature(df, col):
    
    nref = list(df[col])
    queries = list(df["query"])
    dicnref = {}
    for q in range(len(queries)):
        try:
            dicnref[queries[q]] += [nref[q]]
        except:
            dicnref[queries[q]] = [nref[q]]
    for q in dicnref:
        dicnref[q] = sum(dicnref[q])/len(dicnref[q])
    return list(dicnref.values())
def checktimeout(dfg, dfprung):
    queries = dfg["query"]
    total = dfg["TOTAL"]
    queries_prun = dfprung["query"]
    total_prun = dfprung["TOTAL"]
    queries_res = []
    for q in range(len(queries)):
        if total[q] < 0 and total_prun[q] >0:
            queries_res += [queries[q]+"_R"]
        elif total_prun[q] <0 and total[q] > 0:
            queries_res += [queries[q]+"_P"]
        elif total_prun[q] <0 and total[q] <0:
            queries_res += [queries[q]+"_A"]
        else:
            queries_res += [queries[q]]
    return queries_res

    
def checktimeout(dfg, dfprung):
    queries = dfg["query"]
    total = dfg["TOTAL"]
    queries_prun = dfprung["query"]
    total_prun = dfprung["TOTAL"]
    n_ref = dfprung["NREF"]
    queries_res = []
    for q in range(len(queries)):
        if total[q] < 0 and total_prun[q] >0:
            queries_res += [queries[q]+"_R"+"({})".format(n_ref[q])]
        elif total_prun[q] <0 and total[q] > 0:
            queries_res += [queries[q]+"_P"+"({})".format(n_ref[q])]
        elif total_prun[q] <0 and total[q] <0:
            queries_res += [queries[q]+"_A"+"({})".format(n_ref[q])]
        else:
            queries_res += [queries[q]+"({})".format(n_ref[q])]
    return queries_res

def get_emb(path, exp, approach, jucq_exps, optim):
    headers = ["query", "TOTAL"]
    final_df = {}
    queries = []
    for mb in jucq_exps:
        df = pd.read_csv("{}/{}/{}.{}.ref.csv".format(path,mb, exp, approach), sep=",")
        if optim:
            dfopti = pd.read_csv("{}/{}/{}.{}.prune.csv".format(path,mb, exp, approach), sep=",")
        df_ref = average_df(df)
        if optim:
            df_prun = average_df(dfopti)
        if len(queries) == 0:
            queries = df_ref["query"]
        final_df["{}_{}".format(mb, approach)]  = df_ref["TOTAL"]
        if optim:
            final_df["{}_{}optim".format(mb, approach)] = df_prun["TOTAL"]
    final_df["query"] = queries
    return final_df
    
def plotQuery(path, exp="", sgbd = "psql", approaches = ["REF", "PRUN"], optim = True, optim2=True, bdd="LUBM100M", jucq_exps = [], cost=False, selectedqueries=[]):
    
    headers = ["query", "TOTAL"] #"query,EXEC_TIME,NANS,NREF,TREF,NPRUN,TPRUN,TOTAL".split(",")
    final_df = {}
    plotCols = []
    queries = []
    
    for approach in approaches:
        print("Processing reformulation technique {}".format(approach))
        if cost:
            df = pd.read_csv("{}/{}.{}.stats.ref.csv".format(path, exp, approach), sep=",")
        else:
            df = pd.read_csv("{}/{}.{}.ref.csv".format(path, exp, approach), sep=",")
        df_ref = average_df(df, cost=cost)
        # print(df_ref)
        if len(queries) == 0:
            queries = df_ref["query"] 
        final_df["{}_NREF".format(approach)] = get_feature(df, "NREF")
        final_df["{}_TREF".format(approach)] = get_feature(df, "TREF")
        final_df["{}_EXREF".format(approach)] = get_feature(df, "EXEC_TIME")
        final_df["{}/REF".format(approach)]  = df_ref["TOTAL"]
        plotCols +=["{}/REF".format(approach)]
        # print(cost)
        if optim2:
            if cost:
                df = pd.read_csv("{}/{}.{}.stats.db.prune.csv".format(path, exp, approach), sep=",")
            else:
                df = pd.read_csv("{}/{}.{}.db.prune.csv".format(path, exp, approach), sep=",")
            df_prun2 = average_df(df, cost=cost)
            # print(df_prun2)
            final_df["{}_NRPRUNDB".format(approach)] = get_feature(df, "NPRUN")
            final_df["{}_TPRUNDB".format(approach)] = get_feature(df, "TPRUN")
            final_df["{}_EXDB".format(approach)] = get_feature(df, "EXEC_TIME")
            final_df["{}/DB".format(approach)] = df_prun2["TOTAL"]
            plotCols +=["{}/DB".format(approach)]
            
            
        if optim:
            if cost:
                df = pd.read_csv("{}/{}.{}.stats.prune.csv".format(path, exp, approach), sep=",")
                # print(df)
            else:
                df = pd.read_csv("{}/{}.{}.prune.csv".format(path, exp, approach), sep=",")
            df_prun = average_df(df, cost=cost)
            final_df["{}_NRPRUN".format(approach)] = get_feature(df, "NPRUN")
            final_df["{}_TPRUNS".format(approach)] = get_feature(df, "TPRUN")
            final_df["{}_EXPRUN".format(approach)] = get_feature(df, "EXEC_TIME")
            # print(df_prun)
            final_df["{}/S".format(approach)] = df_prun["TOTAL"]
            plotCols +=["{}/S".format(approach)]
            final_df["{}_answer".format(approach)] = get_feature(df, "NANS")
  
    # plotCols += ["{}_{}".format(mb, approach) for mb in jucq_exps]
    # if optim:
    #     plotCols += ["{}_{}/mS".format(mb, approach) for mb in jucq_exps]
    # if optim2:
    #     plotCols += ["{}_{}/D".format(mb, approach) for mb in jucq_exps]

   
    queries_res = []
    approach ="UCQ"
    nrefs = final_df["{}_NREF".format(approaches[0])] 
    dic_q = {}
    if "cc" in path:
        qprefix ="C"
        leg="Consistency Checking"
    else:
        qprefix ="QA"
        leg = "Query Answering"
    for q in range(len(queries)):
        # nrefs = final_df["{}_NREF".format(approach)][q]
        query_ref = []
        for approach in approaches:
            query_ref += [final_df["{}_NREF".format(approach)][q] + 1]
        
        queries_res += ["{}({})".format(queries[q].upper(), "|".join(map(str, query_ref)))]
        dic_q[queries[q]] = "{}({})".format(queries[q].upper(), "|".join(map(str, query_ref)))
    final_df["query"] = queries #queries_res #queries   #
    
    # final_df.update(get_emb(path, exp, approach, jucq_exps, optim))
    
    N = len(final_df)
    final_df = pd.DataFrame(final_df)
    
    
    final_df = final_df.sort_values(by="{}_NREF".format(approaches[0]), ascending=False)
    
    if len(selectedqueries) > 0 :
        select = selectedqueries # [dic_q[q] for q in selectedqueries]
        final_df = final_df[final_df["query"].isin(select)]
        print(list(final_df["query"]))
        queries = list(final_df["query"])
        # final_df["query"] = ["{}{}{}".format(qprefix, i, queries[i][queries[i].index("("):]) for i in range(len(queries))]
        final_df["query"] = ["{}{}".format(qprefix, i) for i in range(len(queries))]
    # df_t = final_df.T
    # print(df_t)
    
    final_df.to_csv("{}/{}.csv".format(path,  exp),index=False, sep=",")
    # all_colors = ["#d7191c", "#fdae61", "#a6611a", "#abd9e9", "#2c7bb6"]
    #543005

    all_colors = ["#bae4b3", "#74c476", "#238b45","#cbc9e2", "#9e9ac8", "#6a51a3", "#fdbe85", "#fd8d3c", "#d94701"  ]
    
    # HSV_tuples = [(x*1.0/N, 0.5, 0.5) for x in range(N)]
    # all_colors = list(map(lambda x: colorsys.hsv_to_rgb(*x), HSV_tuples))
    colors = [all_colors[i] for i in range(len(plotCols))]
    print(colors)
    final_df.plot(x="query", color = colors, y=plotCols, kind="bar",figsize=(9,7))
    # plt.xticks(rotation=-360)
    plt.yscale('log')
    plt.ylim(bottom=10)
    plt.ylabel('Time (ms)')
    plt.title('{} on {} {}'.format(leg, bdd, sgbd))

    plt.legend(ncol=len(approaches))
    # plt.show()
    # plt.tight_layout()
    # poolName = path.split("-")[-1]
    if "MYSQL" in sgbd:
        sgbd = "mysql"
    if "POSTGRES" in sgbd:
        sgbd = "psql"
    if "DB2" in sgbd:
        sgbd = "db2"
    if "qa" in path:
        name = "qa"
    elif "existslimit1" in path:
        name = "existslimit1"
    elif "limit1" in path:
        name= "limit1"
    elif "exists" in path:
        name = "exists"
    elif "cc" in path:
        name = "cc"
    else:
        name = "test"
    # name ="qa"
    plt.savefig("{}/{}_{}_{}.png".format(path, name, exp, sgbd), dpi=500)
    

def oneExp(EXP, BDD, rdbms, _approaches, expsdir="test", _cost=False, target_queries=[]):
    
    if expsdir =="test-dir":
        plotQuery("../exps-final/exps-psql-test/", sgbd=rdbmsdic[rdbms], exp=EXP, approaches=_approaches, optim = True, bdd=BDD, cost=_cost)  
    else:
        plotQuery("../exps-final/exps-{}-{}/{}/".format(rdbms, EXP, expsdir), sgbd=rdbmsdic[rdbms], exp=EXP, approaches=_approaches, optim = True, bdd=BDD, cost=_cost, selectedqueries=target_queries)  

from sklearn.metrics import classification_report

def get_precision_recall(approach, path="dl-sum4qa/exps-final/lubm140_db2/sql"):
    data = []
    y_pred, y_true = [], []
    print("#############   Evaluation on approach {}  #########".format(approach))
    queries = os.listdir(path)
 
    for q in tqdm.tqdm(queries):
        if approach not in q:continue
        name = "{}/{}".format(path, q)
        with open(os.path.join(path, name)) as f:
            data = f.read().split("\n\n")

        for cq in data:
            if "origin" in cq:continue
            try:
                res = int(cq.split("^^")[0])
            except:
                print(cq.split("^^"))
                continue
            if res == 0 or res == 1:
                y_pred += [0]
                y_true += [0]
            elif res == 3:
                y_pred += [1]
                y_true += [1]
            elif res == 2:
                y_pred += [1]
                y_true += [0]
            else:
                print("oups: {}".format(res))
    target_names= ["0", "1"]
    print(classification_report(y_true, y_pred, target_names=target_names))

def get_average(approach, path="dl-sum4qa/exps-final/lubm140_db2/sql"):
    data = []
    y_pred, y_true = [], []
    print("#############   Evaluation on approach {}  #########".format(approach))
    queries = os.listdir(path)
    avg = []
    print('query:ratio')
    for q in tqdm.tqdm(queries):
        if approach not in q:continue
        name = "{}/{}".format(path, q)
        with open(os.path.join(path, name)) as f:
            data = f.read().split("\n\n")
        notfound, total = 0, 0
        for cq in data:
            if "origin" in cq:continue
            try:
                res = int(cq.split("^^")[0])
            except:
                print(cq.split("^^"))
                continue
            if res == 0 or res == 1:
                notfound += 1
                total += 1
            elif res == 2:
                total += 1
        print('{}:{}'.format(q, notfound*100/total))
        avg += [notfound*100/total]
    print("Approach: {}, min: {:.2f}, average: {:.2f}".format(approach, min(avg), sum(avg)/len(avg)))

if __name__ == "__main__":
    
    get_average("UCQ")
    # get_average("USCQ")
    # get_average("JUCQ")
    
    exit()
    test = 1
    _approaches=["UCQ"]
    
    # C0 à C9 : ['iqc0', 'qc0', 'iqc1', 'qc1', 'iqc2', 'qc2', 'iqc3', 'qc3', 'iqc4', 'qc4']
    EXP = "lubm130M"
    BDD = "LUBM140M"
    rdbms="db2"
    qg="vldb-qa"
    # qg="qa-vldb"
    cost = False
    cc = ["qc0", "qc1", "qc2", "qc3", "qc4",  "iqc0", "iqc1", "iqc2", "iqc3", "iqc4"]
    qa = ["q1001", "q1004", "q3001", "q3003", "q3004", "q3005", "q3006", "q3007", "q2004", "q2005"]
    if "cc" in qg:
        target_queries = cc
        _approaches=["UCQ", "USCQ"]
    else:
        target_queries = qa
        _approaches=["UCQ","USCQ", "JUCQ"]
    if test:
        oneExp(EXP, BDD, rdbms, _approaches, expsdir=qg, _cost = cost, target_queries=target_queries)
    else:
        for qg in  ["qa", "cc"]:
            for rdbms in ["psql", "mysql", "db2"]:
                oneExp(EXP, BDD, rdbms, _approaches, expsdir=qg)   
