import argparse
import time


S_INIT = {
    "on(A,B)",
    "on(C,D)",
    "on(E,F)",
    "on(B,T)",
    "on(D,T)",
    "on(F,T)",
    "clear(A)",
    "clear(C)",
    "clear(E)",
}

S_FINAL = {
    "on(B,A)",
    "on(A,E)",
    "on(E,T)",
    "on(F,D)",
    "on(D,C)",
    "on(C,T)",
    "clear(B)",
    "clear(F)",
}

OPERATORS = {
    "move(b,x,y)": {
        "pre": ["on(b,x)", "clear(b)", "clear(y)"],
        "add": ["on(b,y)", "clear(x)"],
        "delete": ["on(b,x)", "clear(y)"],
    },
    "movetotable(b,x)": {
        "pre": ["on(b,x)", "clear(b)"],
        "add": ["on(b,T)", "clear(x)"],
        "delete": ["on(b,x)"],
    },
}

PLAN = [
    {
        "id": "S1",
        "agent": "Bill",
        "action": "move(B,T,A)",
        "pre": ["on(B,T)", "clear(B)", "clear(A)"],
        "add": ["on(B,A)", "clear(T)"],
        "delete": ["on(B,T)", "clear(A)"],
    },
    {
        "id": "S2",
        "agent": "Bill",
        "action": "move(A,B,E)",
        "pre": ["on(A,B)", "clear(A)", "clear(E)"],
        "add": ["on(A,E)", "clear(B)"],
        "delete": ["on(A,B)", "clear(E)"],
    },
    {
        "id": "S3",
        "agent": "Bill",
        "action": "movetotable(E,F)",
        "pre": ["on(E,F)", "clear(E)"],
        "add": ["on(E,T)", "clear(F)"],
        "delete": ["on(E,F)"],
    },
    {
        "id": "S4",
        "agent": "Tom",
        "action": "move(F,T,D)",
        "pre": ["on(F,T)", "clear(F)", "clear(D)"],
        "add": ["on(F,D)", "clear(T)"],
        "delete": ["on(F,T)", "clear(D)"],
    },
    {
        "id": "S5",
        "agent": "Tom",
        "action": "move(D,T,C)",
        "pre": ["on(D,T)", "clear(D)", "clear(C)"],
        "add": ["on(D,C)", "clear(T)"],
        "delete": ["on(D,T)", "clear(C)"],
    },
    {
        "id": "S6",
        "agent": "Tom",
        "action": "movetotable(C,D)",
        "pre": ["on(C,D)", "clear(C)"],
        "add": ["on(C,T)", "clear(D)"],
        "delete": ["on(C,D)"],
    },
]

PARTIAL_ORDER = [
    ("S3", "S2", "S3 produit clear(E), requis par S2"),
    ("S2", "S1", "S2 produit clear(B), requis par S1"),
    ("S6", "S5", "S6 produit clear(D), requis par S5"),
    ("S5", "S4", "S5 produit clear(C), requis par S4"),
    ("S3", "S4", "S3 produit clear(F), requis par S4"),
]

SUBPLANS = {
    "Bill": ["S3", "send(clear(F))", "S2", "S1"],
    "Tom": ["S6", "S5", "wait(clear(F))", "S4"],
}

SIMULATION = [
    ("Bill", "S3", "movetotable(E,F)", "clear(F) devient vrai"),
    ("Tom", "S6", "movetotable(C,D)", "clear(D) devient vrai"),
    ("Tom", "S5", "move(D,T,C)", "on(D,C) devient vrai"),
    ("Bill", "SYNC", "send(clear(F))", "signal envoye a Tom"),
    ("Tom", "SYNC", "wait(clear(F))", "signal recu de Bill"),
    ("Bill", "S2", "move(A,B,E)", "on(A,E) devient vrai"),
    ("Tom", "S4", "move(F,T,D)", "on(F,D) devient vrai"),
    ("Bill", "S1", "move(B,T,A)", "on(B,A) devient vrai"),
]


def step_by_id(step_id):
    for step in PLAN:
        if step["id"] == step_id:
            return step
    raise KeyError(step_id)


def simulate_plan(verbose=True, pause=0.0):
    state = set(S_INIT)

    if verbose:
        print("=== Partie 4: planification centralisee pour plans distribues ===")
        print("Etat initial:", sorted(state))

    for agent, step_id, action, note in SIMULATION:
        if pause:
            time.sleep(pause)

        if step_id != "SYNC":
            step = step_by_id(step_id)
            missing = [fact for fact in step["pre"] if fact not in state and fact != "clear(T)"]
            if missing:
                raise AssertionError(f"Preconditions manquantes pour {step_id}: {missing}")

            for fact in step["delete"]:
                state.discard(fact)
            state.update(step["add"])

        if verbose:
            print(f"{agent:4s} | {step_id:4s} | {action:18s} | {note}")

    missing_goals = sorted(S_FINAL - state)
    if missing_goals:
        raise AssertionError(f"Objectifs non satisfaits: {missing_goals}")

    if verbose:
        print("Etat final atteint:", sorted(S_FINAL))
        print("Verification partie 4: OK")

    return state


def render_streamlit():
    import streamlit as st

    st.set_page_config(page_title="MAS Planning - Partie 4", layout="wide")
    st.title("Planification centralisee pour plans distribues")
    st.caption("Partie 4 - Multi-Agent Systems & Planning")

    tabs = st.tabs(["Etats", "Operateurs", "Plan", "Ordre partiel", "Decomposition", "Simulation"])

    with tabs[0]:
        col1, col2 = st.columns(2)
        col1.subheader("Etat initial")
        col1.write(sorted(S_INIT))
        col2.subheader("Etat final")
        col2.write(sorted(S_FINAL))

    with tabs[1]:
        for name, operator in OPERATORS.items():
            with st.expander(name):
                st.write("Preconditions:", operator["pre"])
                st.write("Add:", operator["add"])
                st.write("Delete:", operator["delete"])

    with tabs[2]:
        for step in PLAN:
            st.write(f"{step['id']} - {step['agent']} - {step['action']}")

    with tabs[3]:
        for before, after, reason in PARTIAL_ORDER:
            st.write(f"{before} < {after}: {reason}")

    with tabs[4]:
        st.write("Bill:", " -> ".join(SUBPLANS["Bill"]))
        st.write("Tom:", " -> ".join(SUBPLANS["Tom"]))

    with tabs[5]:
        if st.button("Lancer la simulation", type="primary"):
            bill_log = []
            tom_log = []
            bill_slot = st.empty()
            tom_slot = st.empty()
            for agent, step_id, action, note in SIMULATION:
                time.sleep(0.4)
                line = f"{step_id} - {action} - {note}"
                if agent == "Bill":
                    bill_log.append(line)
                else:
                    tom_log.append(line)
                bill_slot.write({"Bill": bill_log})
                tom_slot.write({"Tom": tom_log})
            simulate_plan(verbose=False)
            st.success("Etat final atteint et synchronisation respectee.")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--cli", action="store_true", help="Executer la verification en console.")
    args = parser.parse_args()

    if args.cli:
        simulate_plan(verbose=True)
    else:
        render_streamlit()


if __name__ == "__main__":
    main()
