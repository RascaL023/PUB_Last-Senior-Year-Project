#!/usr/bin/env python3
"""
Convert .http API request files to Postman Collection v2.1 JSON.

Usage:
    python scripts/http-to-postman.py
    python scripts/http-to-postman.py --input DIR --output FILE [--verbose]
"""

import argparse
import json
import os
import re
import sys

POSTMAN_SCHEMA = "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
COLLECTION_NAME = "PUB API"

DEFAULT_INPUT = "core/core-app/.assets/api"
DEFAULT_OUTPUT = "core/core-app/.assets/api/postman/PUB-API.postman_collection.json"

HTTP_METHODS = {"GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"}

# Files that should be excluded from the Postman collection
# (kept in the repository but not included in generated output)
EXCLUDED_FILES = {"shadow-problem.http"}

SENSITIVE_KEYWORDS = [
    "token", "password", "secret", "apikey", "authorization",
    "credential", "private", "jwt", "xendit", "refresh", "code",
]


def is_sensitive_var(name):
    lower = name.lower()
    for kw in SENSITIVE_KEYWORDS:
        if kw in lower:
            return True
    return False


def load_env_vars(env_path):
    """Load environment variables from http-client.env.json, sanitizing sensitive values."""
    env_vars = {}
    if not env_path or not os.path.exists(env_path):
        return env_vars
    try:
        with open(env_path, encoding="utf-8") as f:
            data = json.load(f)
        dev = data.get("dev", {})
        for key, value in dev.items():
            if not isinstance(value, str):
                continue
            if is_sensitive_var(key):
                env_vars[key] = ""
            else:
                env_vars[key] = value
    except Exception:
        pass
    return env_vars


def is_separator_line(stripped):
    """Check if a stripped line is a separator: ### ====...===="""
    if not stripped.startswith("###"):
        return False
    content = stripped[3:].strip()
    if not content:
        return False
    if re.match(r'^=+$', content):
        return True
    if re.match(r'^[-=]+[-=]+$', content):
        return True
    return False


def is_group_header(stripped):
    """Check if a stripped line is a group header: ###### NAME ######"""
    if not stripped.startswith("######"):
        return False
    if not stripped.endswith("######"):
        return False
    if len(stripped) <= 12:
        return False
    return True


def parse_method_url(line):
    """Try to parse a line as 'METHOD URL'. Returns (method, url) or None."""
    stripped = line.strip()
    if not stripped:
        return None
    parts = stripped.split(None, 1)
    if len(parts) < 2:
        return None
    method = parts[0].upper()
    if method not in HTTP_METHODS:
        return None
    url = parts[1].strip()
    if not url:
        return None
    return method, url


def split_title_at_em_dash(title):
    """Split a title at em-dash (—) outside parentheses into (name, description_suffix)."""
    depth = 0
    for i, c in enumerate(title):
        if c == '(':
            depth += 1
        elif c == ')':
            depth -= 1
        elif c == '—' and depth == 0:
            name = title[:i].strip()
            desc = title[i + 1:].strip()
            return name, desc
    return title, None


TITLE_KEYWORDS = {
    "Create", "Register", "Login", "Logout", "Refresh", "Open", "Close",
    "Confirm", "Prepare", "Ready", "Complete", "Cancel", "Mark", "Activate",
    "Suspend", "Restore", "Void", "Search", "Get", "Update", "Patch",
    "Delete", "Step", "Guest", "Member", "Webhook", "Dashboard",
    "Add", "Create", "Close", "Get", "Patch", "Delete", "Update", "Login",
    "Logout", "Refresh", "Open", "Close", "Confirm", "Prepare", "Ready",
    "Complete", "Cancel", "Mark", "Activate", "Suspend", "Restore", "Void",
    "Search", "Step", "Guest", "Member", "Webhook", "Dashboard",
    "Create", "Create", "Invalidate", "Expire", "Fail", "Read",
}


def find_title_from_group(all_lines):
    """
    Given all ### lines collected before a METHOD URL, determine the best title.

    Strategy:
    1. Look for lines starting with known action keywords (strong titles)
    2. Use the LAST strong title as the request title
    3. All other lines become description
    4. If no strong title found, use the first line as title (fallback)
    5. If first line looks like a field name (camelCase), use second line, etc.
    """
    if not all_lines:
        return None, []

    strong_titles = []
    for idx, line in enumerate(all_lines):
        first_word = line.split()[0] if line.split() else ""
        # Remove leading parentheses/quotes for keyword matching
        clean = first_word.lstrip('"\'(')
        if clean in TITLE_KEYWORDS or clean.rstrip(':') in TITLE_KEYWORDS:
            strong_titles.append(idx)

    if strong_titles:
        # Use the last strong title as the title
        title_idx = strong_titles[-1]
        title = all_lines[title_idx]
        desc_parts = all_lines[:title_idx] + all_lines[title_idx + 1:]
        return title, desc_parts

    # No strong title found - use heuristics
    # Skip lines that start with lowercase (camelCase field names)
    for idx, line in enumerate(all_lines):
        words = line.split()
        if not words:
            continue
        first_word = words[0]
        # Skip camelCase field names (e.g., "customerId", "invoiceStatus")
        if first_word[0].islower() and any(c.isupper() for c in first_word):
            continue
        # Use this as the title
        title = all_lines[idx]
        desc_parts = all_lines[:idx] + all_lines[idx + 1:]
        return title, desc_parts

    # Fallback: use first line as title
    title = all_lines[0]
    desc_parts = all_lines[1:]
    return title, desc_parts


def collect_body(lines, start_idx):
    """
    Collect body lines from lines list starting at start_idx.
    Returns (body_text, next_index).

    Handles the edge case where body content appears on the same line
    as a ### or ###### marker (e.g., "{}### =========================================").
    """
    body_lines = []
    i = start_idx

    while i < len(lines):
        bline = lines[i]
        bline_stripped = bline.strip()

        if bline_stripped.startswith("###") or bline_stripped.startswith("######"):
            # Check for body content on same line as ### marker
            # e.g., "{}### ========================================="
            match = re.match(r'^(\S.*?)\s*(#{3,}.*)$', bline)
            if match:
                before_hash = match.group(1).rstrip()
                if before_hash.endswith(("}", "{", "]", '"')) or \
                   re.match(r'^-?\d+(\.\d+)?$', before_hash) or \
                   before_hash in ("true", "false", "null"):
                    body_lines.append(before_hash)
                    i += 1  # consume the ### line
                    break
            break

        if bline_stripped == "":
            # Check if this blank line is the end of body
            # (next non-blank is ### or ######)
            k = i + 1
            while k < len(lines) and lines[k].strip() == "":
                k += 1
            if k < len(lines):
                next_stripped = lines[k].strip()
                if next_stripped.startswith("###") or next_stripped.startswith("######"):
                    break
            # Blank line within body
            body_lines.append(bline)
            i += 1
            continue

        body_lines.append(bline)
        i += 1

    body_text = None
    if body_lines:
        body_text = "\n".join(body_lines)
    return body_text, i


def collect_headers(lines, start_idx):
    """Collect headers from lines starting at start_idx. Returns (headers, next_index)."""
    headers = []
    i = start_idx

    while i < len(lines):
        hdr_stripped = lines[i].strip()
        if hdr_stripped == "":
            break
        if hdr_stripped.startswith("###") or hdr_stripped.startswith("######"):
            break
        if ":" in hdr_stripped:
            key, value = hdr_stripped.split(":", 1)
            headers.append({
                "key": key.strip(),
                "value": value.strip()
            })
        i += 1

    return headers, i


def parse_http_file(filepath, verbose=False):
    """
    Parse an .http file and return a list of request dicts.

    Each dict has: {name, method, url, headers, body, description, section}

    Parsing strategy:
    - ###### NAME ###### -> group header, sets current section
    - ### ====...==== -> separator, marks section heading boundary
    - ### text -> potential description/title line
    - METHOD URL line -> start of a request

    For ### lines: they are collected into "groups" separated by blank lines.
    - If a group is immediately followed by a METHOD URL, its first ### line is
      the title and remaining lines are description.
    - If there are multiple groups before a METHOD URL:
      - The last group's first ### line is the title
      - All other ### lines are description
    - If a group is NOT followed by a METHOD URL, its lines are pending
      description that will be attached to the next request (or used as
      section heading if between separators).
    """
    with open(filepath, encoding="utf-8") as f:
        content = f.read()

    lines = content.split("\n")
    requests = []
    current_section = None

    # Pending ### line groups (each group is a list of strings)
    pending_groups = []
    # Current ### group being built (lines without intervening blank lines)
    current_group = []

    def flush_pending():
        """Move current_group to pending_groups and reset."""
        nonlocal pending_groups, current_group
        if current_group:
            pending_groups.append(current_group[:])
        current_group = []

    def commit_section_from_pending():
        """Use pending ### lines as section heading if appropriate."""
        nonlocal pending_groups, current_group, current_section
        flush_pending()
        if pending_groups:
            # Use the first line of the first pending group as section name
            current_section = pending_groups[0][0]
            if verbose:
                print(f"  Section (heading): {current_section}")
        pending_groups = []
        current_group = []

    i = 0
    while i < len(lines):
        stripped = lines[i].strip()

        # Skip empty lines
        if stripped == "":
            # If we have a current_group, flush it (blank line separates groups)
            flush_pending()
            i += 1
            continue

        # Group header: ###### NAME ######
        if is_group_header(stripped):
            commit_section_from_pending()
            inner = stripped.replace("######", "").strip()
            current_section = inner
            if verbose:
                print(f"  Section: {current_section}")
            i += 1
            continue

        # Separator line: ### ====...====
        if is_separator_line(stripped):
            commit_section_from_pending()
            i += 1
            continue

        # ### line (not separator, not group header)
        if stripped.startswith("###"):
            content_part = stripped[3:].strip()
            if not content_part:
                i += 1
                continue

            # Check if next non-blank line is a METHOD URL
            j = i + 1
            while j < len(lines) and lines[j].strip() == "":
                j += 1

            is_request = False
            method = url = None
            if j < len(lines):
                method_url = parse_method_url(lines[j])
                if method_url:
                    method, url = method_url
                    is_request = True

            if is_request:
                # This ### line starts a request group
                current_group.append(content_part)

                # Gather all ### lines collected so far
                all_lines = []
                for g in pending_groups:
                    all_lines.extend(g)
                all_lines.extend(current_group)

                # Determine title and description using heuristic
                title, desc_parts = find_title_from_group(all_lines)

                # Split title at em-dash for name/description
                name, desc_suffix = split_title_at_em_dash(title)
                if desc_suffix:
                    desc_parts.append(desc_suffix)

                desc_text = "\n".join(desc_parts) if desc_parts else ""

                request_name = name
                if not request_name:
                    request_name = f"{method} {url}"

                # Collect headers and body
                i = j + 1
                headers, i = collect_headers(lines, i)

                # Skip blank lines between headers and body
                while i < len(lines) and lines[i].strip() == "":
                    i += 1

                body_text, i = collect_body(lines, i)

                requests.append({
                    "name": request_name,
                    "full_title": title,
                    "method": method,
                    "url": url,
                    "headers": headers,
                    "body": body_text,
                    "description": desc_text if desc_text else None,
                    "section": current_section
                })

                if verbose:
                    print(f"    Request: {request_name} ({method} {url})")

                # Reset pending
                pending_groups = []
                current_group = []
                continue
            else:
                # Not a request, just a description/heading line
                current_group.append(content_part)
                i += 1
                continue

        # Direct METHOD URL without preceding ### title
        method_url = parse_method_url(lines[i])
        if method_url:
            method, url = method_url

            # Build description from pending
            all_lines = []
            for g in pending_groups:
                all_lines.extend(g)
            all_lines.extend(current_group)

            title, desc_parts = find_title_from_group(all_lines)

            name, desc_suffix = split_title_at_em_dash(title) if title else (None, None)
            if desc_suffix:
                desc_parts.append(desc_suffix)

            desc_text = "\n".join(desc_parts) if desc_parts else None

            pending_groups = []
            current_group = []

            i += 1
            headers, i = collect_headers(lines, i)

            while i < len(lines) and lines[i].strip() == "":
                i += 1

            body_text, i = collect_body(lines, i)

            request_name = name if name else f"{method} {url}"

            requests.append({
                "name": request_name,
                "full_title": title,
                "method": method,
                "url": url,
                "headers": headers,
                "body": body_text,
                "description": desc_text,
                "section": current_section
            })

            if verbose:
                print(f"    Request: {request_name} ({method} {url})")
            continue

        # Unknown line type, skip
        i += 1

    return requests, current_section


def parse_url(url_str, verbose=False):
    """
    Convert a URL string to a Postman URL object.

    Only 'raw' and 'query' are populated to avoid breaking variable-based URLs
    like {{baseUrl}}/path or URLs with variables in path segments.
    """
    raw_url = url_str

    url_obj = {"raw": raw_url}

    # Parse query parameters if present
    if "?" in url_str:
        path_part, query_string = url_str.split("?", 1)
        query = []
        for param in query_string.split("&"):
            if "=" in param:
                k, v = param.split("=", 1)
                query.append({"key": k, "value": v})
            elif param:
                query.append({"key": param, "value": ""})
        if query:
            url_obj["query"] = query

    return url_obj


def build_body(body_text):
    """Build Postman body object from raw body text."""
    if not body_text:
        return None

    body_text = body_text.strip()
    if not body_text:
        return None

    # Try to detect if it's JSON
    try:
        json.loads(body_text)
        language = "json"
    except (json.JSONDecodeError, ValueError):
        stripped = body_text
        if (stripped.startswith("{") and stripped.endswith("}")) or \
           (stripped.startswith("[") and stripped.endswith("]")):
            language = "json"
        else:
            language = "text"

    return {
        "mode": "raw",
        "raw": body_text,
        "options": {
            "raw": {
                "language": language
            }
        }
    }


def convert_file_to_folder(filepath, env_vars, verbose=False):
    """Convert a single .http file to a Postman folder item."""
    filename = os.path.basename(filepath)
    # auth.http -> Auth, shadow-problem.http -> Shadow Problem
    folder_name = filename.replace(".http", "").replace("-", " ").title()

    requests, _ = parse_http_file(filepath, verbose)

    folder_item = {
        "name": folder_name,
        "item": []
    }

    # Group requests by section into nested folders
    current_subfolder = None
    seen_names = set()

    for req in requests:
        section = req.get("section")

        if section:
            if section != current_subfolder:
                current_subfolder = section
                subfolder = {
                    "name": section,
                    "item": []
                }
                folder_item["item"].append(subfolder)
                target_list = subfolder["item"]
                seen_names = set()
        else:
            current_subfolder = None
            target_list = folder_item["item"]
            seen_names = set()

        # Disambiguate duplicate names by using full title
        request_name = req["name"]
        if request_name in seen_names and req.get("full_title"):
            request_name = req["full_title"]

        seen_names.add(request_name)

        postman_req = {
            "name": request_name,
            "request": {
                "method": req["method"],
                "header": req["headers"],
                "url": parse_url(req["url"], verbose)
            }
        }

        if req["body"]:
            body = build_body(req["body"])
            if body:
                postman_req["request"]["body"] = body

        if req["description"]:
            postman_req["description"] = req["description"]

        target_list.append(postman_req)

    return folder_item, len(requests)


def generate_collection(input_dir, output_path, verbose=False):
    """Generate Postman collection from .http files."""
    # Load environment variables
    env_path = os.path.join(input_dir, "http-client.env.json")
    env_vars = load_env_vars(env_path)

    if verbose and env_vars:
        print(f"\nLoaded {len(env_vars)} environment variables")
        for k, v in env_vars.items():
            masked = "***" if v == "" else v
            print(f"  {k}: {masked}")

    # Find .http files (non-recursive, sorted, excluding specified files)
    http_files = sorted([
        f for f in os.listdir(input_dir)
        if f.endswith(".http") and f not in EXCLUDED_FILES
    ])

    if verbose and EXCLUDED_FILES:
        excluded = [f for f in os.listdir(input_dir) if f in EXCLUDED_FILES]
        if excluded:
            print(f"\nExcluded files: {excluded}")

    if verbose:
        print(f"\nFound {len(http_files)} .http files: {http_files}")

    # Build collection structure
    collection = {
        "info": {
            "name": COLLECTION_NAME,
            "schema": POSTMAN_SCHEMA
        },
        "variable": [],
        "item": []
    }

    # Add collection variables from env file
    for key, value in sorted(env_vars.items()):
        collection["variable"].append({
            "key": key,
            "value": value,
            "type": "string"
        })

    total_requests = 0
    total_folders = 0
    warnings = []

    for http_file in http_files:
        filepath = os.path.join(input_dir, http_file)
        if verbose:
            print(f"\nProcessing: {http_file}")

        try:
            folder_item, req_count = convert_file_to_folder(filepath, env_vars, verbose)
            collection["item"].append(folder_item)
            total_requests += req_count
            total_folders += 1
        except Exception as e:
            warnings.append(f"Error processing {http_file}: {str(e)}")
            print(f"  WARNING: {http_file}: {str(e)}", file=sys.stderr)

    # Create output directory if needed
    output_dir = os.path.dirname(output_path)
    if output_dir:
        os.makedirs(output_dir, exist_ok=True)

    # Write collection
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(collection, f, indent=2, ensure_ascii=False)

    print(f"\nGenerated Postman Collection")
    print(f"\nOutput:")
    print(f"  {output_path}")
    print(f"\nFiles processed: {len(http_files)}")
    print(f"Requests generated: {total_requests}")
    print(f"Folders generated: {total_folders}")
    print(f"Collection variables: {len(collection['variable'])}")
    print(f"Warnings: {len(warnings)}")

    if warnings:
        for w in warnings:
            print(f"  - {w}")

    return collection


def validate_collection(collection_path):
    """Validate the generated Postman collection JSON."""
    errors = []

    try:
        with open(collection_path, encoding="utf-8") as f:
            data = json.load(f)
    except json.JSONDecodeError as e:
        return [f"Invalid JSON: {e}"]

    # Validate top-level structure
    if "info" not in data:
        errors.append("Missing 'info' at collection root")
    else:
        info = data["info"]
        if "name" not in info:
            errors.append("Missing 'info.name'")
        if "schema" not in info:
            errors.append("Missing 'info.schema'")
        elif info["schema"] != POSTMAN_SCHEMA:
            errors.append(f"Wrong schema: {info['schema']}")

    if "item" not in data:
        errors.append("Missing 'item' at collection root")
    elif not isinstance(data["item"], list):
        errors.append("'item' is not an array")

    if "variable" in data:
        if not isinstance(data["variable"], list):
            errors.append("'variable' is not an array")
        else:
            for i, var in enumerate(data["variable"]):
                if "key" not in var:
                    errors.append(f"Variable at index {i} missing 'key'")

    # Check for non-JSON values (Python objects that shouldn't be there)
    def check_json_values(obj, path="root"):
        if isinstance(obj, dict):
            for k, v in obj.items():
                if v is None:
                    continue
                if not isinstance(v, (str, int, float, bool, list, dict)):
                    errors.append(f"Non-JSON value at {path}.{k}: {type(v).__name__}")
                else:
                    check_json_values(v, f"{path}.{k}")
        elif isinstance(obj, list):
            for i, v in enumerate(obj):
                check_json_values(v, f"{path}[{i}]")

    check_json_values(data)

    # Recursively validate items
    def validate_item(item, path="root"):
        if not isinstance(item, dict):
            errors.append(f"Non-dict item at {path}")
            return

        if "name" not in item:
            errors.append(f"Missing 'name' at {path}")
        elif not isinstance(item["name"], str) or item["name"] == "":
            errors.append(f"Empty or non-string 'name' at {path}")

        if "item" in item:
            if not isinstance(item["item"], list):
                errors.append(f"'item.item' is not array at {path}")
            else:
                for i, sub in enumerate(item["item"]):
                    validate_item(sub, f"{path}.item[{i}]")

        if "request" in item:
            req = item["request"]
            if not isinstance(req, dict):
                errors.append(f"'request' is not dict at {path}")
            else:
                if "method" not in req:
                    errors.append(f"Missing 'request.method' at {path}")
                elif not isinstance(req["method"], str):
                    errors.append(f"'request.method' not string at {path}")

                if "url" not in req:
                    errors.append(f"Missing 'request.url' at {path}")
                elif not isinstance(req["url"], dict):
                    errors.append(f"'request.url' not object at {path}")
                elif "raw" not in req["url"]:
                    errors.append(f"'request.url' missing 'raw' at {path}")

                if "header" in req:
                    if not isinstance(req["header"], list):
                        errors.append(f"'request.header' not array at {path}")
                    else:
                        for h in req["header"]:
                            if "key" not in h:
                                errors.append(f"Header missing 'key' at {path}")
                            if "value" not in h:
                                errors.append(f"Header missing 'value' at {path}")

                if "body" in req:
                    body = req["body"]
                    if "mode" not in body:
                        errors.append(f"Body missing 'mode' at {path}")
                    elif body["mode"] not in ("raw", "urlencoded", "formdata", "file", "graphql", "binary"):
                        errors.append(f"Invalid body mode '{body.get('mode')}' at {path}")

    for item in data.get("item", []):
        validate_item(item)

    return errors


def main():
    parser = argparse.ArgumentParser(
        description="Convert .http API request files to a Postman Collection v2.1 JSON file"
    )
    parser.add_argument(
        "--input",
        default=DEFAULT_INPUT,
        help=f"Input directory containing .http files (default: {DEFAULT_INPUT})"
    )
    parser.add_argument(
        "--output",
        default=DEFAULT_OUTPUT,
        help=f"Output Postman collection JSON path (default: {DEFAULT_OUTPUT})"
    )
    parser.add_argument(
        "--verbose",
        action="store_true",
        help="Enable verbose output"
    )
    args = parser.parse_args()

    # Resolve paths
    input_dir = os.path.abspath(args.input)
    output_path = os.path.abspath(args.output)

    if not os.path.exists(input_dir):
        print(f"Error: Input directory does not exist: {input_dir}", file=sys.stderr)
        sys.exit(1)

    # Generate collection
    generate_collection(input_dir, output_path, verbose=args.verbose)

    # Validate output
    errors = validate_collection(output_path)
    if errors:
        print(f"\nValidation FAILED:", file=sys.stderr)
        for e in errors:
            print(f"  - {e}", file=sys.stderr)
        sys.exit(1)
    else:
        print(f"\nValidation: PASSED")
        print(f"  JSON is valid")
        print(f"  Schema: {POSTMAN_SCHEMA}")


if __name__ == "__main__":
    main()
