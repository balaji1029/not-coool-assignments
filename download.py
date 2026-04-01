base = "https://moodle.iitb.ac.in/pluginfile.php/208648/assignfeedback_file/feedback_files/709158/23b1029_PA1"

tree_structure1 = {
    '23b1029_PA1': {
        'SampleSceneTransform.java': f"{base}/SampleSceneTransform.java?forcedownload=1",
        'Readme.md': f"{base}/Readme.md?forcedownload=1",
        'PA1.java': f"{base}/PA1.java?forcedownload=1",
        'Makefile': f"{base}/Makefile?forcedownload=1",

        'testcases': {
            'Test.java': f"{base}/testcases/Test.java?forcedownload=1",
        },

        'examples': {
            'expectedOutput': {
                'test1.txt': f"{base}/examples/expectedOutput/test1.txt?forcedownload=1",
                'test2.txt': f"{base}/examples/expectedOutput/test2.txt?forcedownload=1",
            },
            'test1': {
                'Test.java': f"{base}/examples/test1/Test.java?forcedownload=1",
            },
            'test2': {
                'Test.java': f"{base}/examples/test2/Test.java?forcedownload=1",
            }
        }
    }
}

base2 = "https://moodle.iitb.ac.in/pluginfile.php/208648/assignfeedback_file/feedback_files/709158/23b1029_PA2"

tree_structure2 = {
    '23b1029_PA2': {
        'AnalysisTransformer.java': f"{base2}/AnalysisTransformer.java?forcedownload=1",
        'Makefile': f"{base2}/Makefile?forcedownload=1",
        'my_output.txt': f"{base2}/my_output.txt?forcedownload=1",
        'PA2.java': f"{base2}/PA2.java?forcedownload=1",
        'test.idk': f"{base2}/test.idk?forcedownload=1",
        'test1.idk': f"{base2}/test1.idk?forcedownload=1",

        'testcases': {
            'Test.java': f"{base2}/testcases/Test.java?forcedownload=1",

            'Test1': {
                'Test.java': f"{base2}/testcases/Test1/Test.java?forcedownload=1",
                'Test1': f"{base2}/testcases/Test1/Test1?forcedownload=1",
            },
            'Test2': {
                'Test.java': f"{base2}/testcases/Test2/Test.java?forcedownload=1",
                'Test2': f"{base2}/testcases/Test2/Test2?forcedownload=1",
            },
            'Test3': {
                'Test.java': f"{base2}/testcases/Test3/Test.java?forcedownload=1",
            },
            'Test4': {
                'Test.java': f"{base2}/testcases/Test4/Test.java?forcedownload=1",
            },
            'Test5': {
                'Test.java': f"{base2}/testcases/Test5/Test.java?forcedownload=1",
            },
            'Test6': {
                'Test.java': f"{base2}/testcases/Test6/Test.java?forcedownload=1",
            },
            'Test7': {
                'Test.java': f"{base2}/testcases/Test7/Test.java?forcedownload=1",
            },
            'Test8': {
                'Test.java': f"{base2}/testcases/Test8/Test.java?forcedownload=1",
            },
        }
    }
}

base3 = "https://moodle.iitb.ac.in/pluginfile.php/208648/assignfeedback_file/feedback_files/709158/23b1029_PA3"

tree_structure3 = {
    '23b1029_PA3': {
        'AnalysisTransformer.java': f"{base3}/AnalysisTransformer.java?forcedownload=1",
        'Makefile': f"{base3}/Makefile?forcedownload=1",
        'PA3.java': f"{base3}/PA3.java?forcedownload=1",
        'readme': f"{base3}/readme?forcedownload=1",

        'testcases': {
            'Test1': {
                'Test.java': f"{base3}/testcases/Test1/Test.java?forcedownload=1",
                'Test': f"{base3}/testcases/Test1/Test?forcedownload=1",
            },

            **{
                f'Test{i}': {
                    'Test.java': f"{base3}/testcases/Test{i}/Test.java?forcedownload=1"
                } for i in range(2, 16)
            }
        }
    }
}


import os
import requests

import browser_cookie3

def create_session():
    session = requests.Session()

    # Automatically grabs cookies from Chrome
    cookies = browser_cookie3.chrome(domain_name='moodle.iitb.ac.in')
    session.cookies.update(cookies)

    return session

def download_file(session, url, path):
    try:
        r = session.get(url, timeout=10)
        r.raise_for_status()

        # Detect login page
        if "Login to the site" in r.text or "<html" in r.text[:200]:
            print(f"⚠ Auth failed (got HTML): {url}")
            return

        with open(path, 'wb') as f:
            f.write(r.content)

        print(f"Downloaded: {path}")

    except Exception as e:
        print(f"Failed: {url}\n   {e}")

def traverse_and_download(tree, session, current_path="."):
    for key, value in tree.items():
        if isinstance(value, dict):
            # It's a directory
            new_path = os.path.join(current_path, key)
            os.makedirs(new_path, exist_ok=True)
            traverse_and_download(value, session, new_path)
        else:
            # It's a file
            save_path = os.path.join(current_path, key)
            download_file(session, value, save_path)

if __name__ == "__main__":
    session = create_session()
    traverse_and_download(tree_structure1, session, )
    traverse_and_download(tree_structure2, session, )
    traverse_and_download(tree_structure3, session, )